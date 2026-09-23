package com.sliit.sparepartshub.web;

import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService service;

    public ProfileController(ProfileService service) {
        this.service = service;
    }

    @GetMapping
    public String profile(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        model.addAttribute("staff", service.getActiveUser(principal.getUser().getUserId()));
        return "profile/index";
    }

    @PostMapping
    public String updateProfile(@AuthenticationPrincipal CustomUserPrincipal principal,
                                @RequestParam String name,
                                @RequestParam String email,
                                RedirectAttributes redirectAttributes) {
        try {
            User updated = service.updateProfile(principal.getUser().getUserId(), name, email);
            principal.getUser().setName(updated.getName());
            principal.getUser().setEmail(updated.getEmail());
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/password")
    public String passwordPage() {
        return "profile/password";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal CustomUserPrincipal principal,
                                 @RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 RedirectAttributes redirectAttributes) {
        try {
            service.changePassword(principal.getUser().getUserId(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/profile";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/profile/password";
        }
    }
}
