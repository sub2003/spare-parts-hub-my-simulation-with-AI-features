package com.sliit.sparepartshub.reporting.controller;

import com.sliit.sparepartshub.entity.AuditReview;
import com.sliit.sparepartshub.reporting.ai.DemandForecastService;
import com.sliit.sparepartshub.reporting.dto.SalesReportData;
import com.sliit.sparepartshub.reporting.service.ReportingService;
import com.sliit.sparepartshub.security.CustomUserPrincipal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/reporting")
public class ReportingController {

    private final ReportingService service;
    private final DemandForecastService demandForecasts;

    public ReportingController(ReportingService service, DemandForecastService demandForecasts) {
        this.service = service;
        this.demandForecasts = demandForecasts;
    }

    @GetMapping
    public String index(Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("summary", service.summary());
        model.addAttribute("topProducts", service.top(today.minusDays(6), today));
        model.addAttribute("criticalProducts", service.criticalProducts());
        model.addAttribute("recentPos", service.recentPurchaseOrders());
        model.addAttribute("audits", service.auditRows(null, null, null, null, null).stream().limit(8).toList());
        model.addAttribute("anomalies", service.anomalies(today.minusDays(6), today));
        model.addAttribute("aiForecast", demandForecasts.dashboard());
        return "reporting/index";
    }

    @PostMapping("/ai/retrain")
    public String retrainDemandForecast(RedirectAttributes redirectAttributes) {
        try {
            redirectAttributes.addFlashAttribute("success", demandForecasts.requestRetraining(true));
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error",
                    "AI retraining could not start: " + ex.getMessage());
        }
        return "redirect:/reporting#ai-demand-forecast";
    }

    @GetMapping("/sales")
    public String sales(@RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        Model model) {
        try {
            SalesReportData report = service.salesReport(from, to);
            model.addAttribute("report", report);
            model.addAttribute("from", from);
            model.addAttribute("to", to);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("report", SalesReportData.empty(from, to));
            model.addAttribute("from", from);
            model.addAttribute("to", to);
        }
        return "reporting/sales";
    }

    @GetMapping({"/sales/export.csv", "/sales.csv"})
    public ResponseEntity<byte[]> csv(@AuthenticationPrincipal CustomUserPrincipal principal,
                                      @RequestParam(required = false) LocalDate from,
                                      @RequestParam(required = false) LocalDate to) {
        try {
            SalesReportData report = service.salesReport(from, to);
            byte[] body = service.salesCsv(report).getBytes(StandardCharsets.UTF_8);
            service.recordReportExport(principal.getUser().getUserId(), "csv", report);
            String filename = "sales-report-" + report.getFrom() + "-to-" + report.getTo() + ".csv";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                    .body(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(ex.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/sales/export.pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal CustomUserPrincipal principal,
                                      @RequestParam(required = false) LocalDate from,
                                      @RequestParam(required = false) LocalDate to) {
        try {
            SalesReportData report = service.salesReport(from, to);
            byte[] body = service.salesPdf(report, principal.getUser().getName());
            service.recordReportExport(principal.getUser().getUserId(), "pdf", report);
            String filename = "sales-report-" + report.getFrom() + "-to-" + report.getTo() + ".pdf";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(body);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8))
                    .body(ex.getMessage().getBytes(StandardCharsets.UTF_8));
        }
    }

    @GetMapping("/audit")
    public String audit(@RequestParam(required = false) String q,
                        @RequestParam(required = false) LocalDate from,
                        @RequestParam(required = false) LocalDate to,
                        @RequestParam(required = false) String action,
                        @RequestParam(required = false) String reviewStatus,
                        Model model) {
        try {
            model.addAttribute("rows", service.auditRows(q, from, to, action, reviewStatus));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("rows", service.auditRows(q, null, null, action, reviewStatus));
        }
        model.addAttribute("actions", service.auditActions());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("action", action == null ? "" : action);
        model.addAttribute("reviewStatus", reviewStatus == null ? "" : reviewStatus);
        return "reporting/audit";
    }

    @GetMapping("/audit/{id}")
    public String auditDetail(@PathVariable Integer id, Model model) {
        model.addAttribute("detail", service.auditDetail(id));
        model.addAttribute("reviewStatuses", AuditReview.ReviewStatus.values());
        return "reporting/audit-detail";
    }

    @PostMapping("/audit/{id}/review")
    public String reviewAudit(@AuthenticationPrincipal CustomUserPrincipal principal,
                              @PathVariable Integer id,
                              @RequestParam AuditReview.ReviewStatus status,
                              @RequestParam(required = false) String note,
                              RedirectAttributes redirectAttributes) {
        try {
            service.reviewAudit(principal.getUser().getUserId(), id, status, note);
            redirectAttributes.addFlashAttribute("success", "Audit review saved.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/reporting/audit/" + id;
    }

    @GetMapping("/anomalies")
    public String anomalies(@RequestParam(required = false) LocalDate from,
                            @RequestParam(required = false) LocalDate to,
                            Model model) {
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(6) : from;
        try {
            model.addAttribute("anomalies", service.anomalies(effectiveFrom, effectiveTo));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("anomalies", service.anomalies(LocalDate.now().minusDays(6), LocalDate.now()));
        }
        model.addAttribute("from", effectiveFrom);
        model.addAttribute("to", effectiveTo);
        return "reporting/anomalies";
    }
}
