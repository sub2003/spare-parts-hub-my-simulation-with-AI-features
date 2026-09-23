package com.sliit.sparepartshub.reporting.controller;

import com.sliit.sparepartshub.reporting.security.CustomSupplierPrincipal;
import com.sliit.sparepartshub.reporting.service.SupplierPortalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/supplier-portal")
public class SupplierPortalController {
    private final SupplierPortalService service;

    public SupplierPortalController(SupplierPortalService service) {
        this.service = service;
    }

    @GetMapping("/login")
    public String login() { return "supplier-portal/login"; }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomSupplierPrincipal principal, Model model) {
        var supplier = service.supplier(principal.getSupplier().getSupplierId());
        var terms = service.terms(supplier.getSupplierId());
        var orders = service.orders(supplier.getSupplierId());
        var offers = service.offers(supplier.getSupplierId());
        var partnerships = service.partnerships(supplier);
        model.addAttribute("supplier", supplier);
        model.addAttribute("terms", terms);
        model.addAttribute("velocity", service.salesVelocity(supplier.getSupplierId()));
        model.addAttribute("orders", orders);
        model.addAttribute("offers", offers);
        model.addAttribute("deletableOfferIds", service.deletableOfferIds(supplier.getSupplierId()));
        model.addAttribute("partnerships", partnerships);
        model.addAttribute("openOrderCount", service.openOrderCount(supplier.getSupplierId()));
        model.addAttribute("activeOfferCount", service.activeOfferCount(supplier.getSupplierId()));
        model.addAttribute("latestPartnership", partnerships.isEmpty() ? null : partnerships.get(0));
        return "supplier-portal/dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomSupplierPrincipal principal, Model model) {
        model.addAttribute("supplier", service.supplier(principal.getSupplier().getSupplierId()));
        return "supplier-portal/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String name, @RequestParam String email, @RequestParam String contact,
                                @AuthenticationPrincipal CustomSupplierPrincipal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            var updated = service.updateProfile(principal.getSupplier().getSupplierId(), name, email, contact);
            principal.getSupplier().setName(updated.getName());
            principal.getSupplier().setEmail(updated.getEmail());
            principal.getSupplier().setContact(updated.getContact());
            redirectAttributes.addFlashAttribute("success", "Supplier profile updated.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/profile";
    }

    @GetMapping("/profile/password")
    public String password() { return "supplier-portal/password"; }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam String currentPassword, @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 @AuthenticationPrincipal CustomSupplierPrincipal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            service.changePassword(principal.getSupplier().getSupplierId(), currentPassword, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
            return "redirect:/supplier-portal/profile";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/supplier-portal/profile/password";
        }
    }

    @GetMapping("/orders/{id}")
    public String order(@PathVariable Integer id, @AuthenticationPrincipal CustomSupplierPrincipal principal, Model model) {
        Integer supplierId = principal.getSupplier().getSupplierId();
        model.addAttribute("po", service.ownedOrder(supplierId, id));
        model.addAttribute("items", service.ownedOrderItems(supplierId, id));
        return "supplier-portal/order";
    }

    @GetMapping("/orders/{id}/export.csv")
    public ResponseEntity<byte[]> exportOrderCsv(@PathVariable Integer id,
                                                  @AuthenticationPrincipal CustomSupplierPrincipal principal) {
        Integer supplierId = principal.getSupplier().getSupplierId();
        var po = service.ownedOrder(supplierId, id);
        var items = service.ownedOrderItems(supplierId, id);
        StringBuilder csv = new StringBuilder("PO Code,Supplier,Status,Product,Ordered,Received,Unit Price\r\n");
        for (var item : items) {
            csv.append(csv(po.getPoCode())).append(',')
                    .append(csv(po.getSupplier().getName())).append(',')
                    .append(csv(po.getStatus().name())).append(',')
                    .append(csv(item.getProduct().getName())).append(',')
                    .append(item.getQuantityOrdered()).append(',')
                    .append(item.getReceivedQuantity()).append(',')
                    .append(item.getPriceAgreed()).append("\r\n");
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + po.getPoCode() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
    }

    @PostMapping("/orders/{id}/shipment")
    public String shipment(@PathVariable Integer id, @RequestParam String trackingReference,
                           @RequestParam(required = false) String shipmentNote,
                           @AuthenticationPrincipal CustomSupplierPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            service.submitShipmentInfo(principal.getSupplier().getSupplierId(), id, trackingReference, shipmentNote);
            redirectAttributes.addFlashAttribute("success", "Shipment information submitted for Admin review.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/orders/" + id;
    }

    @PostMapping("/offers")
    public String offer(@RequestParam Integer productId, @RequestParam BigDecimal price, @RequestParam Integer quantity,
                        @RequestParam(required = false) LocalDate validUntil,
                        @AuthenticationPrincipal CustomSupplierPrincipal principal,
                        RedirectAttributes redirectAttributes) {
        try {
            service.submitOffer(principal.getSupplier(), productId, price, quantity, validUntil);
            redirectAttributes.addFlashAttribute("success", "Restock offer submitted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    @PostMapping("/offers/{id}/delete")
    public String deleteOffer(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomSupplierPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            service.deleteOffer(principal.getSupplier().getSupplierId(), id);
            redirectAttributes.addFlashAttribute("success", "Offer deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    @PostMapping(value = "/partnership", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String partnership(@RequestParam String contactPerson, @RequestParam("catalogFile") MultipartFile catalogFile,
                              @AuthenticationPrincipal CustomSupplierPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            service.submitPartnership(principal.getSupplier(), contactPerson, catalogFile);
            redirectAttributes.addFlashAttribute("success", "Catalog uploaded and partnership request submitted.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    @GetMapping("/partnerships/{id}/catalog")
    public ResponseEntity<org.springframework.core.io.Resource> partnershipCatalog(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomSupplierPrincipal principal) {

        Integer supplierId = principal.getSupplier().getSupplierId();
        var resource = service.catalogResource(supplierId, id);
        String extension = service.catalogExtension(supplierId, id);
        MediaType type = switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "xlsx" -> MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"partnership-" + id + "-catalog." + extension + "\"")
                .contentType(type)
                .body(resource);
    }

    @PostMapping("/partnerships/{id}/withdraw")
    public String withdrawPartnership(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomSupplierPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            service.withdrawPartnership(principal.getSupplier().getSupplierId(), id);
            redirectAttributes.addFlashAttribute(
                    "success", "Partnership request withdrawn successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    @PostMapping("/partnerships/{id}/catalog/delete")
    public String removePartnershipCatalog(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomSupplierPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            service.removePartnershipCatalog(principal.getSupplier().getSupplierId(), id);
            redirectAttributes.addFlashAttribute("success", "Catalogue removed successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    @PostMapping(value = "/partnerships/{id}/catalog/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String replacePartnershipCatalog(
            @PathVariable Integer id,
            @RequestParam("catalogFile") MultipartFile catalogFile,
            @AuthenticationPrincipal CustomSupplierPrincipal principal,
            RedirectAttributes redirectAttributes) {
        try {
            service.replacePartnershipCatalog(
                    principal.getSupplier().getSupplierId(),
                    id,
                    catalogFile);
            redirectAttributes.addFlashAttribute("success", "Catalogue replaced successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/supplier-portal/dashboard";
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
