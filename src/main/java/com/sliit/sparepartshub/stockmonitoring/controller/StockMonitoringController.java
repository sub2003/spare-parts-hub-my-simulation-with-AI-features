package com.sliit.sparepartshub.stockmonitoring.controller;

import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyRow;
import com.sliit.sparepartshub.stockmonitoring.service.StockMonitoringService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/stockmonitoring")
public class StockMonitoringController {

    private final StockMonitoringService service;

    public StockMonitoringController(StockMonitoringService service) {
        this.service = service;
    }

    @GetMapping
    public String index(Model model) {
        List<UrgencyRow> rows = service.dashboardRows();
        List<RestockSuggestion> suggestions = service.suggestions();

        model.addAttribute("rows", rows);
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("criticalCount", rows.stream()
                .filter(row -> "Critical".equals(row.getClassification()))
                .count());
        model.addAttribute("warningCount", rows.stream()
                .filter(row -> "Warning".equals(row.getClassification()))
                .count());
        model.addAttribute("pendingSuggestionCount", suggestions.stream()
                .filter(suggestion -> suggestion.getStatus() == RestockSuggestion.Status.pending)
                .count());
        model.addAttribute("lastRecalculatedAt", service.getLastRecalculatedAt());

        return "stockmonitoring/index";
    }

    @PostMapping("/recalculate")
    public String recalculate(@AuthenticationPrincipal CustomUserPrincipal principal,
                              RedirectAttributes redirectAttributes) {
        try {
            service.recalculateAll(principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Urgency scores recalculated from current stock, sales, customer demand and incoming purchase orders."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stockmonitoring";
    }

    @PostMapping("/suggestions/{id}")
    public String decide(@PathVariable Integer id,
                         @RequestParam RestockSuggestion.Status status,
                         @RequestParam(required = false) Integer quantity,
                         @RequestParam(required = false) String reason,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            service.decide(
                    id,
                    status,
                    quantity,
                    reason,
                    principal.getUser()
            );
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Restock suggestion " + status.name().replace('_', ' ') + "."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stockmonitoring";
    }

    @GetMapping("/stock-requests")
    public String requests(Model model) {
        List<StockRequest> requestRows = service.stockRequests();
        model.addAttribute("requests", requestRows);
        model.addAttribute("products", service.products());
        model.addAttribute("readyCount", requestRows.stream()
                .filter(request -> request.getStatus() == StockRequest.Status.ready_to_notify)
                .count());
        model.addAttribute("pendingCount", requestRows.stream()
                .filter(request -> request.getStatus() == StockRequest.Status.pending)
                .count());
        return "stockmonitoring/stock-requests";
    }

    @PostMapping("/stock-requests")
    public String create(@RequestParam Integer productId,
                         @RequestParam Integer requestedQuantity,
                         @RequestParam String customerName,
                         @RequestParam(required = false) String customerEmail,
                         @RequestParam(required = false) String customerPhonenum,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            StockRequest created = service.createRequest(
                    productId,
                    requestedQuantity,
                    customerName,
                    customerEmail,
                    customerPhonenum,
                    principal.getUser()
            );

            String message = created.getStatus() == StockRequest.Status.ready_to_notify
                    ? "Customer stock request logged. Stock is already available, so it is Ready to Notify."
                    : "Customer stock request logged and is waiting for stock.";
            redirectAttributes.addFlashAttribute("success", message);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stockmonitoring/stock-requests";
    }

    @PostMapping("/stock-requests/{id}/status")
    public String status(@PathVariable Integer id,
                         @RequestParam StockRequest.Status status,
                         @AuthenticationPrincipal CustomUserPrincipal principal,
                         RedirectAttributes redirectAttributes) {
        try {
            service.markRequest(id, status, principal.getUser());
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Stock request updated to " + status.name().replace('_', ' ') + "."
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stockmonitoring/stock-requests";
    }
}
