package com.sliit.sparepartshub.reporting.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.reporting.dto.StaffAccountForm;
import com.sliit.sparepartshub.reporting.repository.StaffHistoryRepository;
import com.sliit.sparepartshub.repository.AuditLogRepository;
import com.sliit.sparepartshub.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StaffAccountService {

    private final UserRepository users;
    private final AuditLogRepository audits;
    private final StaffHistoryRepository staffHistory;
    private final PasswordEncoder passwordEncoder;

    public StaffAccountService(UserRepository users,
                               AuditLogRepository audits,
                               StaffHistoryRepository staffHistory,
                               PasswordEncoder passwordEncoder) {
        this.users = users;
        this.audits = audits;
        this.staffHistory = staffHistory;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> listStaff() {
        return users.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public User get(Integer userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff account was not found."));
    }

    @Transactional(readOnly = true)
    public StaffAccountForm formFor(Integer userId) {
        User user = get(userId);
        StaffAccountForm form = new StaffAccountForm();
        form.setUserCode(user.getUserCode());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        return form;
    }

    @Transactional
    public User create(Integer actorId, StaffAccountForm form) {
        User actor = requireActiveAdmin(actorId);
        String code = normalizeCode(form.getUserCode());
        String name = requireText(form.getName(), "Name", 50);
        String email = normalizeEmail(form.getEmail());
        User.Role role = requireRole(form.getRole());
        validatePassword(form.getTemporaryPassword());

        if (users.existsByUserCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("That staff code is already in use.");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("That email address is already in use.");
        }

        User user = new User();
        user.setUserCode(code);
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(form.getTemporaryPassword()));
        user.setActive(true);
        users.save(user);

        saveAudit(actor, "ACCOUNT_CREATED", user.getUserId(), null,
                snapshot(user));
        return user;
    }

    @Transactional
    public User update(Integer actorId, Integer targetId, StaffAccountForm form) {
        User actor = requireActiveAdmin(actorId);
        User target = get(targetId);
        String code = normalizeCode(form.getUserCode());
        String name = requireText(form.getName(), "Name", 50);
        String email = normalizeEmail(form.getEmail());
        User.Role newRole = requireRole(form.getRole());

        if (users.existsByUserCodeIgnoreCaseAndUserIdNot(code, targetId)) {
            throw new IllegalArgumentException("That staff code is already in use.");
        }
        if (users.existsByEmailIgnoreCaseAndUserIdNot(email, targetId)) {
            throw new IllegalArgumentException("That email address is already in use.");
        }

        if (actor.getUserId().equals(target.getUserId()) && newRole != User.Role.admin) {
            throw new IllegalStateException("You cannot change your own Admin role while signed in. Ask another Admin to make that change.");
        }
        if (target.isActive()
                && target.getRole() == User.Role.admin
                && newRole != User.Role.admin
                && users.countByRoleAndActiveTrue(User.Role.admin) <= 1) {
            throw new IllegalStateException("At least one active Admin must remain.");
        }

        String before = snapshot(target);
        User.Role previousRole = target.getRole();
        target.setUserCode(code);
        target.setName(name);
        target.setEmail(email);
        target.setRole(newRole);
        users.save(target);

        saveAudit(actor, "ACCOUNT_UPDATED", target.getUserId(), before, snapshot(target));
        if (previousRole != newRole) {
            saveAudit(actor, "ROLE_CHANGED", target.getUserId(),
                    "{\"role\":" + json(previousRole.name()) + "}",
                    "{\"role\":" + json(newRole.name()) + "}");
        }
        return target;
    }

    @Transactional
    public void setActive(Integer actorId, Integer targetId, boolean active) {
        User actor = requireActiveAdmin(actorId);
        User target = get(targetId);

        if (!active && actor.getUserId().equals(target.getUserId())) {
            throw new IllegalStateException("You cannot deactivate your own currently signed-in Admin account.");
        }
        if (!active
                && target.isActive()
                && target.getRole() == User.Role.admin
                && users.countByRoleAndActiveTrue(User.Role.admin) <= 1) {
            throw new IllegalStateException("At least one active Admin must remain.");
        }
        if (target.isActive() == active) {
            return;
        }

        boolean previous = target.isActive();
        target.setActive(active);
        users.save(target);
        saveAudit(actor, active ? "ACCOUNT_ACTIVATED" : "ACCOUNT_DEACTIVATED", target.getUserId(),
                "{\"active\":" + previous + "}",
                "{\"active\":" + active + "}");
    }

    @Transactional
    public void resetPassword(Integer actorId, Integer targetId, String temporaryPassword) {
        User actor = requireActiveAdmin(actorId);
        User target = get(targetId);
        validatePassword(temporaryPassword);
        target.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        users.save(target);
        saveAudit(actor, "PASSWORD_RESET", target.getUserId(), null,
                "{\"temporaryPasswordIssued\":true}");
    }


    @Transactional(readOnly = true)
    public long activeAdminCount() {
        return users.countByRoleAndActiveTrue(User.Role.admin);
    }

    @Transactional
    public void delete(Integer actorId, Integer targetId) {
        User actor = requireActiveAdmin(actorId);
        User target = get(targetId);

        if (actor.getUserId().equals(target.getUserId())) {
            throw new IllegalStateException("You cannot delete your own account while signed in.");
        }

        if (target.isActive() && target.getRole() == User.Role.admin) {
            List<User> activeAdmins = users.findByRoleAndActiveTrue(User.Role.admin);
            if (activeAdmins.size() <= 1) {
                throw new IllegalStateException("The last active Admin account cannot be deleted.");
            }
        }

        List<String> blockers = staffHistory.blockingHistory(targetId);
        if (!blockers.isEmpty()) {
            throw new IllegalStateException(
                    "Staff account cannot be deleted because business history exists ("
                            + String.join(", ", blockers)
                            + "). Deactivate the account instead."
            );
        }

        String before = snapshot(target);
        try {
            users.delete(target);
            users.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Staff account cannot be deleted because related business history still exists. Deactivate the account instead.",
                    ex
            );
        }

        saveAudit(actor, "STAFF_ACCOUNT_DELETED", targetId, before, "{\"deleted\":true}");
    }

    private User requireActiveAdmin(Integer actorId) {
        User actor = get(actorId);
        if (!actor.isActive()) {
            throw new IllegalStateException("This account is inactive.");
        }
        if (actor.getRole() != User.Role.admin) {
            throw new IllegalStateException("Administrator permission is required.");
        }
        return actor;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Temporary password must contain at least 8 characters.");
        }
    }

    private User.Role requireRole(User.Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        return role;
    }

    private String normalizeCode(String value) {
        String code = requireText(value, "Staff code", 20).toUpperCase();
        if (!code.matches("[A-Z0-9_-]+")) {
            throw new IllegalArgumentException("Staff code may contain only letters, numbers, underscore and hyphen.");
        }
        return code;
    }

    private String normalizeEmail(String email) {
        String value = requireText(email, "Email", 254).toLowerCase();
        if (!value.contains("@") || value.startsWith("@") || value.endsWith("@")) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        return value;
    }

    private String requireText(String value, String label, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new IllegalArgumentException(label + " is too long.");
        }
        return clean;
    }

    private void saveAudit(User actor, String action, Integer recordId, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setActionType(action);
        log.setTableName("users");
        log.setRecordId(recordId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        audits.save(log);
    }

    private String snapshot(User user) {
        return "{"
                + "\"userCode\":" + json(user.getUserCode()) + ","
                + "\"name\":" + json(user.getName()) + ","
                + "\"email\":" + json(user.getEmail()) + ","
                + "\"role\":" + json(user.getRole().name()) + ","
                + "\"active\":" + user.isActive()
                + "}";
    }

    private String json(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
