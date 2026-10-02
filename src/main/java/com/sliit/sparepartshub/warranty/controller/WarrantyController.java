package com.sliit.sparepartshub.warranty.controller;

import com.sliit.sparepartshub.entity.RmaClaim;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.warranty.service.WarrantyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/warranty")
public class WarrantyController {

    private final WarrantyService service;

    public WarrantyController(WarrantyService service) {
        this.service = service;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String serial,
                        @RequestParam(required = false) RmaClaim.ClaimStatus status,
                        Model model) {
        populateDashboard(model, status);
        model.addAttribute("selectedStatus", status);

        if (serial != null && !serial.isBlank()) {
            try {
                var lookup = service.lookup(serial);
                model.addAttribute("lookup", lookup);
                model.addAttribute("history", service.history(lookup.getSerial().getSerialId()));
            } catch (IllegalArgumentException ex) {
                model.addAttribute("error", ex.getMessage());
            }
        }
        return "warranty/index";
    }

    @GetMapping("/lookup")
    public String lookup(@RequestParam String serial, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("serial", serial);
        return "redirect:/warranty";
    }

    @GetMapping("/rmas")
    public String list(@RequestParam(required = false) RmaClaim.ClaimStatus status,
                       RedirectAttributes redirectAttributes) {
        if (status != null) {
            redirectAttributes.addAttribute("status", status.name());
        }
        return "redirect:/warranty";
    }

    @GetMapping("/rmas/{id}")
    public String details(@PathVariable Integer id, Model model) {
        try {
            var claim = service.getClaim(id);
            model.addAttribute("claim", claim);
            model.addAttribute("canDeletePendingClaim", service.canDeletePendingClaim(claim));
            model.addAttribute("history", service.history(claim.getSerial().getSerialId()));
            model.addAttribute("replacementOptions", service.replacementOptions(id));
            if (claim.getSerial().getSale() != null
                    && claim.getSerial().getSale().getSoldAt() != null
                    && claim.getSerial().getProduct().getWarrantyPeriodMonths() != null
                    && claim.getSerial().getProduct().getWarrantyPeriodMonths() > 0) {
                model.addAttribute(
                        "warrantyExpiry",
                        claim.getSerial().getSale().getSoldAt().toLocalDate()
                                .plusMonths(claim.getSerial().getProduct().getWarrantyPeriodMonths())
                );
            }
            return "warranty/rma-details";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            populateDashboard(model, null);
            return "warranty/index";
        }
    }

    @PostMapping("/claims")
    public String create(@RequestParam String serialValue,
                         @RequestParam String faultDescription,
                         @RequestParam(required = false) String conditionNotes,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            var claim = service.create(
                    serialValue,
                    faultDescription,
                    conditionNotes,
                    principal.getUser()
            );
            redirectAttributes.addFlashAttribute("success",
                    "RMA " + claim.getClaimCode() + " created and is awaiting review.");
            return "redirect:/warranty/rmas/" + claim.getClaimId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addAttribute("serial", serialValue);
            return "redirect:/warranty";
        }
    }

    @PostMapping("/rmas/{id}/delete")
    public String delete(@PathVariable Integer id,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            String code = service.deletePendingClaim(id, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "RMA " + code
                    + " deleted. The original serial was restored to sold status.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", "RMA could not be deleted safely. No changes were made.");
        }
        return "redirect:/warranty";
    }

    @PostMapping("/rmas/{id}/approve")
    public String approve(@PathVariable Integer id,
                          @AuthenticationPrincipal CustomUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            service.approve(id, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "RMA approved. Select a final resolution.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warranty/rmas/" + id;
    }

    @PostMapping("/rmas/{id}/reject")
    public String reject(@PathVariable Integer id,
                         @RequestParam String rejectionReason,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            service.reject(id, rejectionReason, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "RMA rejected. No inventory stock was changed.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warranty/rmas/" + id;
    }

    @PostMapping("/rmas/{id}/replace")
    public String replace(@PathVariable Integer id,
                          @RequestParam Integer replacementSerialId,
                          @RequestParam(required = false) String resolutionNotes,
                          @RequestParam(defaultValue = "false") boolean confirm,
                          @AuthenticationPrincipal CustomUserPrincipal principal,
                          RedirectAttributes redirectAttributes) {
        try {
            service.replace(id, replacementSerialId, resolutionNotes, confirm, principal.getUser());
            redirectAttributes.addFlashAttribute("success",
                    "Replacement completed. Sellable stock was reduced exactly once.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warranty/rmas/" + id;
    }

    @PostMapping("/rmas/{id}/refund")
    public String refund(@PathVariable Integer id,
                         @RequestParam(required = false) String resolutionNotes,
                         @RequestParam(defaultValue = "false") boolean confirm,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            service.refund(id, resolutionNotes, confirm, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "RMA recorded as refunded.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warranty/rmas/" + id;
    }

    @PostMapping("/rmas/{id}/manufacturer")
    public String manufacturer(@PathVariable Integer id,
                               @RequestParam(required = false) String resolutionNotes,
                               @RequestParam(defaultValue = "false") boolean confirm,
                               @AuthenticationPrincipal CustomUserPrincipal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            service.sendToManufacturer(id, resolutionNotes, confirm, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "RMA recorded as sent to manufacturer.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/warranty/rmas/" + id;
    }

    private void populateDashboard(Model model, RmaClaim.ClaimStatus status) {
        model.addAttribute("pendingCount", service.pendingCount());
        model.addAttribute("approvedCount", service.approvedCount());
        model.addAttribute("todayCount", service.todayCount());
        model.addAttribute("claims", service.claims(status));
    }
}
