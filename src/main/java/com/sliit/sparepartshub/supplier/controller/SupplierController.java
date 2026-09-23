package com.sliit.sparepartshub.supplier.controller;

import com.sliit.sparepartshub.entity.PartnershipRequest;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.Supplier;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.reporting.service.CatalogStorageService;
import com.sliit.sparepartshub.supplier.dto.CreatePurchaseOrderForm;
import com.sliit.sparepartshub.supplier.dto.CreateSupplierRequest;
import com.sliit.sparepartshub.supplier.dto.ReceiveShipmentForm;
import com.sliit.sparepartshub.supplier.dto.SupplierComparisonRow;
import com.sliit.sparepartshub.supplier.dto.SupplierProductAssignmentForm;
import com.sliit.sparepartshub.supplier.service.SupplierManagementService;
import com.sliit.sparepartshub.supplier.service.SupplierFieldValidationException;
import com.sliit.sparepartshub.supplier.service.SupplierProductValidationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/supplier")
public class SupplierController {

    private final SupplierManagementService service;
    private final CatalogStorageService catalogStorage;

    public SupplierController(SupplierManagementService service, CatalogStorageService catalogStorage) {
        this.service = service;
        this.catalogStorage = catalogStorage;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute(
                "supplierCount",
                service.getSuppliers().stream()
                        .filter(Supplier::isActive)
                        .count()
        );

        model.addAttribute(
                "partnershipCount",
                service.getPartnershipRequests().stream()
                        .filter(request -> request.getStatus() == PartnershipRequest.Status.pending)
                        .count()
        );

        model.addAttribute(
                "restockCount",
                service.getApprovedRestockSuggestions().size()
        );

        model.addAttribute(
                "poCount",
                service.getPurchaseOrders().stream()
                        .filter(order -> order.getStatus() != PurchaseOrder.Status.received)
                        .count()
        );

        return "supplier/index";
    }

    @GetMapping("/records")
    public String supplierRecords(Model model) {
        model.addAttribute("suppliers", service.getSuppliers());
        return "supplier/suppliers";
    }

    @GetMapping("/records/new")
    public String newSupplier(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new CreateSupplierRequest());
        }
        return "supplier/supplier-create";
    }

    @PostMapping("/records")
    public String createSupplier(
            @ModelAttribute("form") CreateSupplierRequest form,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Supplier supplier = service.createSupplier(form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Supplier created successfully. " + supplier.getName()
                            + " can now use the Supplier Portal when the account is active."
            );
            return "redirect:/supplier/records";

        } catch (SupplierFieldValidationException ex) {
            bindingResult.rejectValue(ex.getField(), "supplier." + ex.getField(), ex.getMessage());
            model.addAttribute("error", "Please correct the highlighted supplier information.");
            return "supplier/supplier-create";

        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "supplier/supplier-create";
        }
    }

    @GetMapping("/records/{id}/products")
    public String manageSupplierProducts(@PathVariable Integer id, Model model) {
        Supplier supplier = service.getSupplier(id);
        model.addAttribute("supplier", supplier);
        model.addAttribute("form", service.getSupplierProductAssignmentForm(id));
        model.addAttribute("linkedProductCount", service.getSupplierProductCount(id));
        return "supplier/supplier-products";
    }

    @PostMapping("/records/{id}/products")
    public String updateSupplierProducts(
            @PathVariable Integer id,
            @ModelAttribute("form") SupplierProductAssignmentForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            service.updateSupplierProducts(id, form, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Supplier products and commercial terms updated successfully.");
            return "redirect:/supplier/records/" + id + "/products";

        } catch (SupplierProductValidationException ex) {
            service.refreshSupplierProductFormLabels(form);
            bindingResult.rejectValue(
                    "rows[" + ex.getRowIndex() + "]." + ex.getField(),
                    "supplierProduct." + ex.getField(),
                    ex.getMessage());
            model.addAttribute("supplier", service.getSupplier(id));
            model.addAttribute("linkedProductCount", service.getSupplierProductCount(id));
            model.addAttribute("error", "Please correct the highlighted supplier product terms.");
            return "supplier/supplier-products";

        } catch (RuntimeException ex) {
            service.refreshSupplierProductFormLabels(form);
            model.addAttribute("supplier", service.getSupplier(id));
            model.addAttribute("linkedProductCount", service.getSupplierProductCount(id));
            model.addAttribute("error", ex.getMessage());
            return "supplier/supplier-products";
        }
    }

    @PostMapping("/records/{supplierId}/products/{productId}/remove")
    public String removeSupplierProduct(
            @PathVariable Integer supplierId,
            @PathVariable Integer productId,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.removeSupplierProduct(supplierId, productId, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Product removed from supplier successfully."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/supplier/records/" + supplierId + "/products";
    }

    @GetMapping("/records/{id}/edit")
    public String editSupplier(@PathVariable Integer id, Model model) {
        model.addAttribute("supplier", service.getSupplier(id));
        return "supplier/supplier-form";
    }

    @PostMapping("/records/{id}")
    public String updateSupplier(
            @PathVariable Integer id,
            @RequestParam(required = false) String supplierCode,
            @RequestParam String name,
            @RequestParam String contact,
            @RequestParam String email,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.updateSupplier(
                    id,
                    supplierCode,
                    name,
                    contact,
                    email,
                    principal.getUser()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Supplier record updated."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/records";
    }

    @PostMapping("/records/{id}/active")
    public String updateSupplierActiveStatus(
            @PathVariable Integer id,
            @RequestParam boolean active,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.setSupplierActive(
                    id,
                    active,
                    principal.getUser()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Supplier status updated."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/records";
    }

    @PostMapping("/records/{id}/delete")
    public String deleteSupplier(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.deleteSupplier(id, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Supplier deleted successfully."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/records";
    }

    @GetMapping("/partnerships")
    public String partnerships(Model model) {
        model.addAttribute(
                "requests",
                service.getPartnershipRequests()
        );

        return "supplier/partnership-requests";
    }

    @GetMapping("/partnerships/{id}/catalog")
    public ResponseEntity<Resource> partnershipCatalog(@PathVariable Integer id) {
        PartnershipRequest request = service.getPartnershipRequest(id);
        Resource resource = catalogStorage.load(request.getCatalogFilePath());
        String extension = catalogStorage.extensionOfStoredName(request.getCatalogFilePath());
        MediaType type = switch (extension) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "csv" -> MediaType.parseMediaType("text/csv");
            case "xlsx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"partnership-" + id + "-catalog." + extension + "\"")
                .contentType(type)
                .body(resource);
    }

    @PostMapping("/partnerships/{id}/decision")
    public String partnershipDecision(
            @PathVariable Integer id,
            @RequestParam PartnershipRequest.Status decision,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.decidePartnership(
                    id,
                    decision,
                    principal.getUser()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Partnership request " + decision.name() + "."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/partnerships";
    }

    @PostMapping("/partnerships/{id}/withdraw")
    public String withdrawPartnership(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.withdrawPartnershipRequest(id, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Partnership request withdrawn successfully."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/supplier/partnerships";
    }

    @PostMapping("/partnerships/{id}/catalog/delete")
    public String deletePartnershipCatalog(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.removePartnershipCatalog(id, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Catalogue removed successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/supplier/partnerships";
    }

    @GetMapping("/offers")
    public String offers(Model model) {
        model.addAttribute("offers", service.getRestockOffers());
        model.addAttribute("deletableOfferIds", service.getDeletableOfferIds());
        return "supplier/offers";
    }

    @PostMapping("/offers/{id}/delete")
    public String deleteOffer(
            @PathVariable Integer id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.deleteOffer(id, principal.getUser());
            redirectAttributes.addFlashAttribute("success", "Offer deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/supplier/offers";
    }

    @GetMapping("/restock-requirements")
    public String restockRequirements(Model model) {
        model.addAttribute(
                "suggestions",
                service.getApprovedRestockSuggestions()
        );

        return "supplier/restock-requirements";
    }

    @GetMapping("/compare")
    public String compare(
            @RequestParam Integer suggestionId,
            @RequestParam(required = false) Integer quantity,
            Model model) {

        RestockSuggestion suggestion =
                service.getRestockSuggestion(suggestionId);

        int requiredQuantity =
                quantity == null
                        ? suggestion.getSuggestedQuantity()
                        : quantity;

        List<SupplierComparisonRow> rows =
                service.compareSuppliers(
                        suggestion.getProduct().getProductId(),
                        requiredQuantity
                );

        model.addAttribute("suggestion", suggestion);
        model.addAttribute("quantity", requiredQuantity);
        model.addAttribute("rows", rows);

        return "supplier/compare";
    }

    @GetMapping("/purchase-orders/new")
    public String newPurchaseOrder(
            @RequestParam Integer supplierId,
            @RequestParam Integer suggestionId,
            @RequestParam Integer quantity,
            @RequestParam BigDecimal price,
            Model model) {

        RestockSuggestion suggestion =
                service.getRestockSuggestion(suggestionId);

        Supplier supplier =
                service.getSupplier(supplierId);

        CreatePurchaseOrderForm form =
                new CreatePurchaseOrderForm();

        form.setSupplierId(supplierId);
        form.setProductId(
                suggestion.getProduct().getProductId()
        );
        form.setQuantity(quantity);
        form.setAgreedPrice(price);
        form.setSuggestionId(suggestionId);

        model.addAttribute("form", form);
        model.addAttribute("supplier", supplier);
        model.addAttribute("suggestion", suggestion);

        return "supplier/purchase-order-form";
    }

    @PostMapping("/purchase-orders")
    public String createPurchaseOrder(
            @ModelAttribute("form") CreatePurchaseOrderForm form,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            PurchaseOrder purchaseOrder =
                    service.createPurchaseOrder(
                            form,
                            principal.getUser()
                    );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Purchase order "
                            + purchaseOrder.getPoCode()
                            + " created."
            );

            return "redirect:/supplier/purchase-orders/"
                    + purchaseOrder.getPoId();

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );

            return "redirect:/supplier/restock-requirements";
        }
    }

    @GetMapping("/purchase-orders")
    public String purchaseOrders(Model model) {
        model.addAttribute(
                "purchaseOrders",
                service.getPurchaseOrders()
        );

        return "supplier/purchase-orders";
    }

    @GetMapping("/purchase-orders/{id}")
    public String purchaseOrderDetail(
            @PathVariable Integer id,
            Model model) {

        PurchaseOrder purchaseOrder =
                service.getPurchaseOrder(id);

        List<PurchaseOrderItem> items =
                service.getPurchaseOrderItems(id);

        BigDecimal total =
                items.stream()
                        .map(item ->
                                item.getPriceAgreed()
                                        .multiply(
                                                BigDecimal.valueOf(
                                                        item.getQuantityOrdered()
                                                )
                                        )
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        model.addAttribute("po", purchaseOrder);
        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "supplier/purchase-order-detail";
    }

    /*
     * The simulation version of SupplierManagementService.markShipped(...)
     * expects:
     *
     *     (Integer poId, LocalDate expectedDeliveryDate, User actor)
     *
     * The previous controller called it with only:
     *
     *     (Integer poId, User actor)
     *
     * which caused the compilation error shown in IntelliJ.
     */
    @PostMapping("/purchase-orders/{id}/ship")
    public String markShipped(
            @PathVariable Integer id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate expectedDeliveryDate,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.markShipped(
                    id,
                    expectedDeliveryDate,
                    principal.getUser()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Purchase order marked as shipped."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/purchase-orders/" + id;
    }

    @GetMapping("/purchase-orders/{id}/receive")
    public String receiveForm(
            @PathVariable Integer id,
            Model model) {

        PurchaseOrder purchaseOrder =
                service.getPurchaseOrder(id);

        List<PurchaseOrderItem> items =
                service.getPurchaseOrderItems(id);

        model.addAttribute("po", purchaseOrder);
        model.addAttribute("items", items);
        model.addAttribute(
                "form",
                new ReceiveShipmentForm()
        );

        return "supplier/receive-shipment";
    }

    @PostMapping("/purchase-orders/{id}/receive")
    public String receive(
            @PathVariable Integer id,
            @RequestParam Integer poItemId,
            @ModelAttribute ReceiveShipmentForm form,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            RedirectAttributes redirectAttributes) {

        try {
            service.receiveSingleItemShipment(
                    id,
                    poItemId,
                    form,
                    principal.getUser()
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Shipment receipt recorded and inventory updated."
            );

        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    ex.getMessage()
            );
        }

        return "redirect:/supplier/purchase-orders/" + id;
    }

    @GetMapping("/purchase-orders/{id}/export.csv")
    public ResponseEntity<byte[]> exportCsv(
            @PathVariable Integer id) {

        PurchaseOrder purchaseOrder =
                service.getPurchaseOrder(id);

        List<PurchaseOrderItem> items =
                service.getPurchaseOrderItems(id);

        StringBuilder csv =
                new StringBuilder(
                        "PO Code,Supplier,Product,Ordered,Received,"
                                + "Unit Price,Line Total,Status\n"
                );

        for (PurchaseOrderItem item : items) {

            BigDecimal lineTotal =
                    item.getPriceAgreed()
                            .multiply(
                                    BigDecimal.valueOf(
                                            item.getQuantityOrdered()
                                    )
                            );

            csv.append(csvCell(purchaseOrder.getPoCode()))
                    .append(',')
                    .append(csvCell(
                            purchaseOrder.getSupplier().getName()
                    ))
                    .append(',')
                    .append(csvCell(
                            item.getProduct().getName()
                    ))
                    .append(',')
                    .append(item.getQuantityOrdered())
                    .append(',')
                    .append(item.getReceivedQuantity())
                    .append(',')
                    .append(item.getPriceAgreed())
                    .append(',')
                    .append(lineTotal)
                    .append(',')
                    .append(purchaseOrder.getStatus())
                    .append('\n');
        }

        byte[] data =
                csv.toString()
                        .getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + purchaseOrder.getPoCode()
                                + ".csv\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "text/csv"
                        )
                )
                .body(data);
    }

    private String csvCell(String value) {
        String safeValue =
                value == null
                        ? ""
                        : value.replace("\"", "\"\"");

        return "\"" + safeValue + "\"";
    }
}
