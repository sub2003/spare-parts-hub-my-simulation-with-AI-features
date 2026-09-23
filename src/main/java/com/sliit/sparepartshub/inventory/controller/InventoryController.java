package com.sliit.sparepartshub.inventory.controller;

import com.sliit.sparepartshub.entity.PickTicket;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.StorageLocation;
import com.sliit.sparepartshub.inventory.dto.CreateProductForm;
import com.sliit.sparepartshub.inventory.dto.EditProductForm;
import com.sliit.sparepartshub.inventory.dto.LocationForm;
import com.sliit.sparepartshub.inventory.service.InventoryService;
import com.sliit.sparepartshub.inventory.service.ProductValidationException;
import com.sliit.sparepartshub.inventory.service.QrCodeService;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService service;
    private final QrCodeService qrCodeService;

    public InventoryController(InventoryService service, QrCodeService qrCodeService) {
        this.service = service;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("totalProducts", service.totalProductCount());
        model.addAttribute("lowStockCount", service.lowStockCount());
        model.addAttribute("outOfStockCount", service.outOfStockCount());
        model.addAttribute("serialTrackedCount", service.serialTrackedProductCount());
        model.addAttribute("pendingCount", service.pendingCount());
        model.addAttribute("partialCount", service.partialCount());
        model.addAttribute("completedToday", service.completedToday());
        model.addAttribute("locationCount", service.locationCount());
        model.addAttribute("lowStock", service.lowStock());
        model.addAttribute("recentCompleted", service.recentCompletedTickets());
        return "inventory/index";
    }

    @GetMapping("/products")
    public String products(Model model) {
        model.addAttribute("rows", service.productRows());
        return "inventory/products";
    }


    @GetMapping("/products/new")
    public String newProduct(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CreateProductForm());
        }
        populateProductFormModel(model);
        return "inventory/product-create";
    }

    @PostMapping("/products")
    public String createProduct(@ModelAttribute("form") CreateProductForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Check the product form for invalid numeric values.");
            populateProductFormModel(model);
            return "inventory/product-create";
        }
        try {
            Product created = service.createProduct(form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success", "Product " + created.getProductCode() + " created successfully."
            );
            return "redirect:/inventory/products";
        } catch (ProductValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("errorField", e.getField());
            populateProductFormModel(model);
            return "inventory/product-create";
        } catch (Exception e) {
            model.addAttribute("error", "Product could not be created. No changes were saved.");
            populateProductFormModel(model);
            return "inventory/product-create";
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editProduct(@PathVariable Integer id, Model model) {
        Product product = service.getProduct(id);
        model.addAttribute("product", product);
        model.addAttribute("form", service.editProductForm(id));
        populateProductFormModel(model);
        return "inventory/product-edit";
    }

    @PostMapping("/products/{id}")
    public String updateProduct(@PathVariable Integer id,
                                @ModelAttribute("form") EditProductForm form,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal CustomUserPrincipal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        Product product = service.getProduct(id);
        model.addAttribute("product", product);
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Check the product form for invalid numeric values.");
            populateProductFormModel(model);
            return "inventory/product-edit";
        }
        try {
            Product updated = service.updateProduct(id, form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success", "Product " + updated.getProductCode() + " updated successfully."
            );
            return "redirect:/inventory/products";
        } catch (ProductValidationException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("errorField", e.getField());
            model.addAttribute("product", service.getProduct(id));
            populateProductFormModel(model);
            return "inventory/product-edit";
        } catch (Exception e) {
            model.addAttribute("error", "Product could not be updated. No changes were saved.");
            model.addAttribute("product", service.getProduct(id));
            populateProductFormModel(model);
            return "inventory/product-edit";
        }
    }

    @PostMapping("/products/{id}/reorder-level")
    public String updateReorderLevel(@PathVariable Integer id,
                                     @RequestParam Integer reorderLevel,
                                     @AuthenticationPrincipal CustomUserPrincipal principal,
                                     RedirectAttributes redirectAttributes) {
        try {
            service.updateReorderLevel(id, reorderLevel, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Reorder level updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory/products";
    }

    @GetMapping("/pick-tickets")
    public String tickets(Model model) {
        model.addAttribute("tickets", service.allTickets());
        return "inventory/pick-tickets";
    }

    @GetMapping("/pick-tickets/scan")
    public String scan(@RequestParam(required = false) String code, Model model) {
        if (code != null && !code.isBlank()) {
            try {
                PickTicket ticket = service.findProcessableByCode(code);
                return "redirect:/inventory/pick-tickets/" + ticket.getTicketId();
            } catch (RuntimeException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return "inventory/scan";
    }

    @PostMapping("/pick-tickets/scan")
    public String scanImage(@RequestParam("qrImage") MultipartFile qrImage, Model model) {
        try {
            String code = qrCodeService.decode(qrImage);
            PickTicket ticket = service.findProcessableByCode(code);
            return "redirect:/inventory/pick-tickets/" + ticket.getTicketId();
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "inventory/scan";
        }
    }

    @GetMapping("/pick-tickets/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        model.addAttribute("ticket", service.getTicket(id));
        model.addAttribute("lines", service.ticketLines(id));
        return "inventory/pick-ticket-detail";
    }

    @GetMapping(value = "/pick-tickets/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> ticketQr(@PathVariable Integer id) {
        PickTicket ticket = service.getTicket(id);
        byte[] png = qrCodeService.png(ticket.getTicketCode(), 320);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping("/pick-tickets/{id}/complete")
    public String complete(@PathVariable Integer id,
                           @RequestParam Map<String, String> params,
                           @AuthenticationPrincipal CustomUserPrincipal principal,
                           RedirectAttributes redirectAttributes) {
        try {
            Map<Integer, Integer> picks = new HashMap<>();
            Map<Integer, String> pickedSerials = new HashMap<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey().startsWith("picked_")) {
                    Integer itemId = Integer.valueOf(entry.getKey().substring(7));
                    Integer quantity = Integer.valueOf(entry.getValue());
                    picks.put(itemId, quantity);
                } else if (entry.getKey().startsWith("serials_")) {
                    Integer itemId = Integer.valueOf(entry.getKey().substring(8));
                    pickedSerials.put(itemId, entry.getValue());
                }
            }
            service.completeTicket(id, picks, pickedSerials, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Pick ticket processed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory/pick-tickets/" + id;
    }

    @GetMapping("/locations")
    public String locations(Model model) {
        model.addAttribute("products", service.products());
        model.addAttribute("locations", service.locations());
        return "inventory/locations";
    }

    @GetMapping("/locations/new")
    public String newLocation(Model model) {
        model.addAttribute("form", new LocationForm());
        model.addAttribute("editing", false);
        return "inventory/location-form";
    }

    @PostMapping("/locations")
    public String createLocation(@ModelAttribute("form") LocationForm form,
                                 @AuthenticationPrincipal CustomUserPrincipal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            StorageLocation created = service.createLocation(form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success", "Storage location " + created.getLocationCode() + " created."
            );
            return "redirect:/inventory/locations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventory/locations/new";
        }
    }

    @GetMapping("/locations/{id}/edit")
    public String editLocation(@PathVariable Integer id, Model model) {
        model.addAttribute("location", service.getLocation(id));
        model.addAttribute("form", service.locationForm(id));
        model.addAttribute("editing", true);
        return "inventory/location-form";
    }

    @PostMapping("/locations/{id}")
    public String updateLocation(@PathVariable Integer id,
                                 @ModelAttribute("form") LocationForm form,
                                 @AuthenticationPrincipal CustomUserPrincipal principal,
                                 RedirectAttributes redirectAttributes) {
        try {
            StorageLocation updated = service.updateLocation(id, form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success", "Storage location " + updated.getLocationCode() + " updated."
            );
            return "redirect:/inventory/locations";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/inventory/locations/" + id + "/edit";
        }
    }

    @GetMapping(value = "/locations/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> locationQr(@PathVariable Integer id) {
        StorageLocation location = service.getLocation(id);
        byte[] png = qrCodeService.png(location.getLocationCode(), 320);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping("/locations/assign")
    public String assign(@RequestParam Integer productId,
                         @RequestParam Integer locationId,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            service.assignLocation(productId, locationId, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Storage location updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/inventory/locations";
    }
    private void populateProductFormModel(Model model) {
        model.addAttribute("locations", service.locations());
        model.addAttribute("categories", service.productCategories());
        model.addAttribute("brands", service.productBrands());
    }

}
