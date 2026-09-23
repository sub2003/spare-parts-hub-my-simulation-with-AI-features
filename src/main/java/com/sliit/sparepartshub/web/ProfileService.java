package com.sliit.sparepartshub.web;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.repository.AuditLogRepository;
import com.sliit.sparepartshub.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private final UserRepository users;
    private final AuditLogRepository audits;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserRepository users,
                          AuditLogRepository audits,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.audits = audits;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User getActiveUser(Integer userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Staff account was not found."));
        if (!user.isActive()) {
            throw new IllegalStateException("This account is inactive.");
        }
        return user;
    }

    @Transactional
    public User updateProfile(Integer userId, String name, String email) {
        User user = getActiveUser(userId);
        String cleanName = requireText(name, "Name", 50);
        String cleanEmail = normalizeEmail(email);

        if (users.existsByEmailIgnoreCaseAndUserIdNot(cleanEmail, userId)) {
            throw new IllegalArgumentException("That email address is already used by another staff account.");
        }

        String oldValue = "{\"name\":" + json(user.getName()) + ",\"email\":" + json(user.getEmail()) + "}";
        user.setName(cleanName);
        user.setEmail(cleanEmail);
        users.save(user);

        saveAudit(user, "PROFILE_UPDATED", user.getUserId(), oldValue,
                "{\"name\":" + json(user.getName()) + ",\"email\":" + json(user.getEmail()) + "}");
        return user;
    }

    @Transactional
    public void changePassword(Integer userId,
                               String currentPassword,
                               String newPassword,
                               String confirmPassword) {
        User user = getActiveUser(userId);
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        validateNewPassword(newPassword, confirmPassword);
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        users.save(user);
        saveAudit(user, "PASSWORD_CHANGED", user.getUserId(), null,
                "{\"passwordChanged\":true}");
    }

    private void validateNewPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must contain at least 8 characters.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }
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

    private String json(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
