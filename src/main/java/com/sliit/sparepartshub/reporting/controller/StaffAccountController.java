package com.sliit.sparepartshub.reporting.controller;

import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.reporting.dto.StaffAccountForm;
import com.sliit.sparepartshub.reporting.service.StaffAccountService;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reporting/staff")
public class StaffAccountController {

    private final StaffAccountService service;

    public StaffAccountController(StaffAccountService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        model.addAttribute("staff", service.listStaff());
        model.addAttribute("currentUserId", principal.getUser().getUserId());
        model.addAttribute("activeAdminCount", service.activeAdminCount());
        return "reporting/staff-list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("editing", false);
        model.addAttribute("form", new StaffAccountForm());
        model.addAttribute("roles", User.Role.values());
        return "reporting/staff-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal CustomUserPrincipal principal,
                         @ModelAttribute StaffAccountForm form,
                         RedirectAttributes redirectAttributes) {
        try {
            User created = service.create(principal.getUser().getUserId(), form);
            redirectAttributes.addFlashAttribute("success", "Staff account " + created.getUserCode() + " created successfully.");
            return "redirect:/reporting/staff";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/reporting/staff/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("editing", true);
        model.addAttribute("staff", service.get(id));
        model.addAttribute("form", service.formFor(id));
        model.addAttribute("roles", User.Role.values());
        return "reporting/staff-form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal CustomUserPrincipal principal,
                         @PathVariable Integer id,
                         @ModelAttribute StaffAccountForm form,
                         RedirectAttributes redirectAttributes) {
        try {
            service.update(principal.getUser().getUserId(), id, form);
            redirectAttributes.addFlashAttribute("success", "Staff account updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporting/staff/" + id + "/edit";
    }

    @PostMapping("/{id}/active")
    public String setActive(@AuthenticationPrincipal CustomUserPrincipal principal,
                            @PathVariable Integer id,
                            @RequestParam boolean active,
                            RedirectAttributes redirectAttributes) {
        try {
            service.setActive(principal.getUser().getUserId(), id, active);
            redirectAttributes.addFlashAttribute("success", active ? "Account activated." : "Account deactivated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporting/staff";
    }


    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal CustomUserPrincipal principal,
                         @PathVariable Integer id,
                         RedirectAttributes redirectAttributes) {
        try {
            service.delete(principal.getUser().getUserId(), id);
            redirectAttributes.addFlashAttribute("success", "Staff account deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporting/staff";
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@AuthenticationPrincipal CustomUserPrincipal principal,
                                @PathVariable Integer id,
                                @RequestParam String temporaryPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            service.resetPassword(principal.getUser().getUserId(), id, temporaryPassword);
            redirectAttributes.addFlashAttribute("success", "Temporary password set successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporting/staff/" + id + "/edit";
    }
}
