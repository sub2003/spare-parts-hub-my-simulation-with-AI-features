package com.sliit.sparepartshub.reporting.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.AuditReview;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.RmaClaim;
import com.sliit.sparepartshub.entity.Sale;
import com.sliit.sparepartshub.entity.SaleItem;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.reporting.dto.AnomalyRow;
import com.sliit.sparepartshub.reporting.dto.AuditDetailView;
import com.sliit.sparepartshub.reporting.dto.AuditRow;
import com.sliit.sparepartshub.reporting.dto.ReportSummary;
import com.sliit.sparepartshub.reporting.dto.SalesReportData;
import com.sliit.sparepartshub.reporting.dto.TopProductRow;
import com.sliit.sparepartshub.reporting.repository.ReportingAuditLogRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingAuditReviewRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingProductRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingPurchaseOrderRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingRmaRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingSaleItemRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingSaleRepository;
import com.sliit.sparepartshub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private static final BigDecimal CRITICAL_URGENCY = BigDecimal.valueOf(70);
    private static final LocalDate EARLIEST_REPORT_DATE = LocalDate.of(1970, 1, 1);
    private static final Pattern SENSITIVE_JSON_VALUE = Pattern.compile(
            "(?i)(\\\"(?:password(?:_?hash)?|temporaryPassword|temporary_password|token|secret|credential|dbPassword|databasePassword)\\\"\\s*:\\s*)(\\\"(?:\\\\.|[^\\\"\\\\])*\\\"|[^,}\\s]+)"
    );
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[A-Za-z0-9./]{53}");

    private final ReportingSaleRepository sales;
    private final ReportingSaleItemRepository items;
    private final ReportingProductRepository products;
    private final ReportingPurchaseOrderRepository purchaseOrders;
    private final ReportingRmaRepository rmas;
    private final ReportingAuditLogRepository audits;
    private final ReportingAuditReviewRepository reviews;
    private final UserRepository users;

    public ReportingService(ReportingSaleRepository sales,
                            ReportingSaleItemRepository items,
                            ReportingProductRepository products,
                            ReportingPurchaseOrderRepository purchaseOrders,
                            ReportingRmaRepository rmas,
                            ReportingAuditLogRepository audits,
                            ReportingAuditReviewRepository reviews,
                            UserRepository users) {
        this.sales = sales;
        this.items = items;
        this.products = products;
        this.purchaseOrders = purchaseOrders;
        this.rmas = rmas;
        this.audits = audits;
        this.reviews = reviews;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ReportSummary summary() {
        ReportSummary result = new ReportSummary();
        LocalDate today = LocalDate.now();
        result.setTodaySales(zero(sales.total(today.atStartOfDay(), today.plusDays(1).atStartOfDay())));
        result.setWeekSales(zero(sales.total(today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay())));
        result.setTotalRevenue(zero(sales.totalAll()));
        result.setTotalSales(sales.count());
        Long quantity = items.totalQuantity();
        result.setTotalQuantitySold(quantity == null ? 0 : quantity);
        result.setOpenPos(purchaseOrders.countByStatusIn(List.of(
                PurchaseOrder.Status.pending,
                PurchaseOrder.Status.shipped,
                PurchaseOrder.Status.partially_received
        )));
        result.setPendingRmas(rmas.countByClaimStatusIn(List.of(
                RmaClaim.ClaimStatus.pending,
                RmaClaim.ClaimStatus.approved
        )));
        result.setCriticalProducts(products.countByUrgencyScoreGreaterThanEqual(CRITICAL_URGENCY));
        return result;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> recentPurchaseOrders() {
        return purchaseOrders.findTop5ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public SalesReportData salesReport(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate today = LocalDate.now();
        LocalDate from = requestedFrom == null ? earliestSaleDate().orElse(today) : requestedFrom;
        LocalDate to = requestedTo == null ? today : requestedTo;
        validateRange(from, to);

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        List<Sale> saleRows = sales.range(start, endExclusive);
        List<SaleItem> lineRows = items.range(start, endExclusive);
        BigDecimal totalRevenue = saleRows.stream()
                .map(Sale::getAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long quantitySold = lineRows.stream()
                .map(SaleItem::getQuantity)
                .filter(v -> v != null)
                .mapToLong(Integer::longValue)
                .sum();
        BigDecimal average = saleRows.isEmpty()
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(saleRows.size()), 2, RoundingMode.HALF_UP);

        return new SalesReportData(
                from,
                to,
                saleRows,
                lineRows,
                topFromItems(lineRows),
                saleRows.size(),
                quantitySold,
                totalRevenue,
                average
        );
    }

    private Optional<LocalDate> earliestSaleDate() {
        return sales.earliestSoldAt().map(LocalDateTime::toLocalDate);
    }

    public void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return;
        }
        if (from.isBefore(EARLIEST_REPORT_DATE)) {
            throw new IllegalArgumentException("The report start date is outside the supported range.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("From date must be on or before To date.");
        }
    }

    @Transactional(readOnly = true)
    public List<TopProductRow> top(LocalDate from, LocalDate to) {
        validateRange(from, to);
        return topFromItems(items.range(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }

    private List<TopProductRow> topFromItems(List<SaleItem> source) {
        Map<String, Long> quantity = new HashMap<>();
        Map<String, BigDecimal> revenue = new HashMap<>();

        for (SaleItem item : source) {
            String key = item.getProduct().getProductCode() + "|" + item.getProduct().getName();
            quantity.merge(key, item.getQuantity().longValue(), Long::sum);
            revenue.merge(
                    key,
                    item.getPriceAtSale().multiply(BigDecimal.valueOf(item.getQuantity())),
                    BigDecimal::add
            );
        }

        return quantity.entrySet().stream()
                .map(entry -> {
                    String[] parts = entry.getKey().split("\\|", 2);
                    String label = parts.length == 2 ? parts[1] : entry.getKey();
                    return new TopProductRow(label, entry.getValue(),
                            revenue.getOrDefault(entry.getKey(), BigDecimal.ZERO));
                })
                .sorted(Comparator.comparingLong(TopProductRow::getQuantity).reversed()
                        .thenComparing(TopProductRow::getName))
                .limit(10)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Product> criticalProducts() {
        return products.findAll().stream()
                .filter(product -> product.getUrgencyScore() != null
                        && product.getUrgencyScore().compareTo(CRITICAL_URGENCY) >= 0)
                .sorted(Comparator.comparing(Product::getUrgencyScore).reversed())
                .limit(8)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditRow> auditRows(String query,
                                    LocalDate from,
                                    LocalDate to,
                                    String action,
                                    String reviewStatus) {
        if (from != null && to != null) {
            validateRange(from, to);
        }

        List<AuditLog> source = audits.findTop500ByOrderByLoggedAtDesc();
        List<Integer> ids = source.stream().map(AuditLog::getLogId).toList();
        Map<Integer, AuditReview> reviewByLog = ids.isEmpty()
                ? Map.of()
                : reviews.findByAuditLog_LogIdIn(ids).stream()
                .collect(Collectors.toMap(r -> r.getAuditLog().getLogId(), Function.identity()));

        LocalDate anomalyFrom = LocalDate.now().minusDays(6);
        Set<Integer> anomalyIds = anomalies(anomalyFrom, LocalDate.now()).stream()
                .flatMap(a -> a.getAuditLogIds().stream())
                .collect(Collectors.toSet());

        String needle = blank(query);
        String actionNeedle = blank(action);
        String reviewNeedle = blank(reviewStatus);

        return source.stream()
                .filter(log -> from == null || (log.getLoggedAt() != null && !log.getLoggedAt().toLocalDate().isBefore(from)))
                .filter(log -> to == null || (log.getLoggedAt() != null && !log.getLoggedAt().toLocalDate().isAfter(to)))
                .filter(log -> needle == null || matchesAudit(log, needle.toLowerCase(Locale.ROOT)))
                .filter(log -> actionNeedle == null || log.getActionType().equalsIgnoreCase(actionNeedle))
                .map(log -> new AuditRow(log, reviewByLog.get(log.getLogId()), anomalyIds.contains(log.getLogId())))
                .filter(row -> reviewNeedle == null || row.getReviewStatus().equalsIgnoreCase(reviewNeedle))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> auditActions() {
        return audits.findTop500ByOrderByLoggedAtDesc().stream()
                .map(AuditLog::getActionType)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private boolean matchesAudit(AuditLog row, String needle) {
        return contains(row.getActionType(), needle)
                || contains(row.getTableName(), needle)
                || contains(row.getRecordId() == null ? null : row.getRecordId().toString(), needle)
                || (row.getUser() != null && contains(row.getUser().getName(), needle));
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    @Transactional(readOnly = true)
    public AuditDetailView auditDetail(Integer logId) {
        AuditLog log = audits.findByLogId(logId)
                .orElseThrow(() -> new IllegalArgumentException("Audit event was not found."));
        AuditReview review = reviews.findByAuditLog_LogId(logId).orElse(null);
        return new AuditDetailView(log, review, prettyAndRedact(log.getOldValue()), prettyAndRedact(log.getNewValue()));
    }

    @Transactional
    public AuditReview reviewAudit(Integer actorId,
                                   Integer logId,
                                   AuditReview.ReviewStatus status,
                                   String note) {
        User actor = requireActiveAdmin(actorId);
        AuditLog target = audits.findByLogId(logId)
                .orElseThrow(() -> new IllegalArgumentException("Audit event was not found."));
        if (status == null) {
            throw new IllegalArgumentException("Review status is required.");
        }
        String cleanNote = blank(note);
        if (status == AuditReview.ReviewStatus.action_required && cleanNote == null) {
            throw new IllegalArgumentException("A review note is required when action is required.");
        }
        if (cleanNote != null && cleanNote.length() > 500) {
            throw new IllegalArgumentException("Review note must be 500 characters or fewer.");
        }

        AuditReview review = reviews.findByAuditLog_LogId(logId).orElseGet(AuditReview::new);
        review.setAuditLog(target);
        review.setReviewedBy(actor);
        review.setReviewStatus(status);
        review.setReviewNote(cleanNote);
        review.setReviewedAt(LocalDateTime.now());
        review = reviews.save(review);

        // Record the administrator decision, but never recursively create a new
        // audit event while reviewing a review-audit event itself.
        if (target.getActionType() == null || !target.getActionType().startsWith("AUDIT_")) {
            String action = switch (status) {
                case reviewed -> "AUDIT_REVIEWED";
                case dismissed -> "AUDIT_DISMISSED";
                case action_required -> "AUDIT_ACTION_REQUIRED";
            };
            saveAudit(actor, action, "audit_review", review.getReviewId(), null,
                    "{\"auditLogId\":" + logId
                            + ",\"status\":" + json(status.name())
                            + ",\"note\":" + json(cleanNote) + "}");
        }
        return review;
    }

    @Transactional(readOnly = true)
    public List<AnomalyRow> anomalies(LocalDate requestedFrom, LocalDate requestedTo) {
        LocalDate to = requestedTo == null ? LocalDate.now() : requestedTo;
        LocalDate from = requestedFrom == null ? to.minusDays(6) : requestedFrom;
        validateRange(from, to);
        List<AuditLog> logs = audits.findByLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtAsc(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());

        List<AnomalyRow> result = new ArrayList<>();
        addCompatibilityOverrideBursts(logs, result);
        addRepeatedProductEdits(logs, result);
        addSensitiveAccountBursts(logs, result);

        return result.stream()
                .sorted(Comparator.comparing(AnomalyRow::getEndedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void addCompatibilityOverrideBursts(List<AuditLog> logs, List<AnomalyRow> out) {
        Map<Integer, List<AuditLog>> byActor = logs.stream()
                .filter(log -> "SALE_COMPATIBILITY_OVERRIDE".equals(log.getActionType()))
                .filter(log -> log.getUser() != null)
                .collect(Collectors.groupingBy(log -> log.getUser().getUserId(), LinkedHashMap::new, Collectors.toList()));
        for (List<AuditLog> actorLogs : byActor.values()) {
            burst(actorLogs, 3, Duration.ofMinutes(30)).ifPresent(window -> {
                AuditLog first = window.get(0);
                AuditLog last = window.get(window.size() - 1);
                out.add(new AnomalyRow(
                        "REPEATED_COMPATIBILITY_OVERRIDES",
                        "warning",
                        window.size() + " compatibility overrides by the same staff user within 30 minutes.",
                        first.getUser().getName(),
                        "sale",
                        window.stream().map(v -> String.valueOf(v.getRecordId())).collect(Collectors.joining(", ")),
                        first.getLoggedAt(),
                        last.getLoggedAt(),
                        window.size() + " SALE_COMPATIBILITY_OVERRIDE audit events.",
                        window.stream().map(AuditLog::getLogId).toList()
                ));
            });
        }
    }

    private void addRepeatedProductEdits(List<AuditLog> logs, List<AnomalyRow> out) {
        Map<Integer, List<AuditLog>> byProduct = logs.stream()
                .filter(log -> "PRODUCT_UPDATED".equals(log.getActionType()))
                .filter(log -> log.getRecordId() != null)
                .collect(Collectors.groupingBy(AuditLog::getRecordId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Integer, List<AuditLog>> entry : byProduct.entrySet()) {
            burst(entry.getValue(), 5, Duration.ofMinutes(60)).ifPresent(window -> {
                AuditLog first = window.get(0);
                AuditLog last = window.get(window.size() - 1);
                String actor = window.stream().map(AuditLog::getUser).filter(v -> v != null)
                        .map(User::getName).distinct().collect(Collectors.joining(", "));
                out.add(new AnomalyRow(
                        "REPEATED_PRODUCT_EDITS",
                        "warning",
                        "Product " + entry.getKey() + " was edited " + window.size() + " times within 60 minutes.",
                        actor.isBlank() ? "Unknown" : actor,
                        "product",
                        String.valueOf(entry.getKey()),
                        first.getLoggedAt(),
                        last.getLoggedAt(),
                        window.size() + " PRODUCT_UPDATED audit events for the same product.",
                        window.stream().map(AuditLog::getLogId).toList()
                ));
            });
        }
    }

    private void addSensitiveAccountBursts(List<AuditLog> logs, List<AnomalyRow> out) {
        Set<String> sensitive = Set.of(
                "ACCOUNT_CREATED", "ACCOUNT_UPDATED", "ROLE_CHANGED",
                "ACCOUNT_ACTIVATED", "ACCOUNT_DEACTIVATED", "PASSWORD_RESET"
        );
        Map<Integer, List<AuditLog>> byActor = logs.stream()
                .filter(log -> sensitive.contains(log.getActionType()))
                .filter(log -> log.getUser() != null)
                .collect(Collectors.groupingBy(log -> log.getUser().getUserId(), LinkedHashMap::new, Collectors.toList()));
        for (List<AuditLog> actorLogs : byActor.values()) {
            burst(actorLogs, 3, Duration.ofMinutes(60)).ifPresent(window -> {
                AuditLog first = window.get(0);
                AuditLog last = window.get(window.size() - 1);
                out.add(new AnomalyRow(
                        "SENSITIVE_ACCOUNT_ACTIVITY",
                        "critical",
                        window.size() + " sensitive staff-account changes by the same administrator within 60 minutes.",
                        first.getUser().getName(),
                        "users",
                        window.stream().map(v -> String.valueOf(v.getRecordId())).distinct().collect(Collectors.joining(", ")),
                        first.getLoggedAt(),
                        last.getLoggedAt(),
                        window.stream().map(AuditLog::getActionType).collect(Collectors.joining(", ")),
                        window.stream().map(AuditLog::getLogId).toList()
                ));
            });
        }
    }

    private Optional<List<AuditLog>> burst(List<AuditLog> source, int threshold, Duration duration) {
        if (source.size() < threshold) {
            return Optional.empty();
        }
        List<AuditLog> sorted = source.stream()
                .filter(v -> v.getLoggedAt() != null)
                .sorted(Comparator.comparing(AuditLog::getLoggedAt))
                .toList();
        int left = 0;
        for (int right = 0; right < sorted.size(); right++) {
            while (left < right
                    && Duration.between(sorted.get(left).getLoggedAt(), sorted.get(right).getLoggedAt()).compareTo(duration) > 0) {
                left++;
            }
            if (right - left + 1 >= threshold) {
                return Optional.of(new ArrayList<>(sorted.subList(left, right + 1)));
            }
        }
        return Optional.empty();
    }

    public String salesCsv(SalesReportData report) {
        StringBuilder csv = new StringBuilder();
        csv.append("Sale Code,Sold At,Salesperson,Product Code,Product,Quantity,Original Unit Price,Discount Per Unit,Final Unit Price,Discount Reason,Line Total,Sale Total\n");
        for (SaleItem item : report.getItems()) {
            csv.append(csvCell(item.getSale().getSaleCode())).append(',')
                    .append(csvCell(String.valueOf(item.getSale().getSoldAt()))).append(',')
                    .append(csvCell(item.getSale().getSoldBy().getName())).append(',')
                    .append(csvCell(item.getProduct().getProductCode())).append(',')
                    .append(csvCell(item.getProduct().getName())).append(',')
                    .append(item.getQuantity()).append(',')
                    .append(item.getOriginalUnitPrice()).append(',')
                    .append(item.getDiscountAmount()).append(',')
                    .append(item.getPriceAtSale()).append(',')
                    .append(csvCell(item.getDiscountReason())).append(',')
                    .append(item.getLineTotal()).append(',')
                    .append(item.getSale().getAmount()).append('\n');
        }
        return csv.toString();
    }

    public byte[] salesPdf(SalesReportData report, String generatedBy) {
        List<String> lines = new ArrayList<>();
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        lines.add("SPARE PARTS HUB");
        lines.add("Sales Report");
        lines.add("Period: " + report.getFrom() + " to " + report.getTo() + " (inclusive)");
        lines.add("Generated by: " + (generatedBy == null ? "Admin" : generatedBy));
        lines.add("Generated at: " + LocalDateTime.now().format(dt));
        lines.add("");
        lines.add("Summary");
        lines.add("Sales: " + report.getSaleCount());
        lines.add("Revenue: LKR " + money(report.getTotalRevenue()));
        lines.add("Average sale: LKR " + money(report.getAverageSaleValue()));
        lines.add("Quantity sold: " + report.getQuantitySold());
        lines.add("");
        lines.add("Sales");
        lines.add("Code | Date | Salesperson | Amount (LKR)");
        for (Sale sale : report.getSales()) {
            lines.add(trimPdf(sale.getSaleCode(), 16) + " | "
                    + sale.getSoldAt().format(dt) + " | "
                    + trimPdf(sale.getSoldBy().getName(), 24) + " | "
                    + money(sale.getAmount()));
        }
        lines.add("");
        lines.add("Top Products");
        lines.add("Product | Qty | Revenue (LKR)");
        for (TopProductRow row : report.getTopProducts()) {
            lines.add(trimPdf(row.getName(), 38) + " | " + row.getQuantity() + " | " + money(row.getRevenue()));
        }
        lines.add("");
        lines.add("End of report");
        return SimplePdfWriter.textReport(lines);
    }

    @Transactional
    public void recordReportExport(Integer actorId, String format, SalesReportData report) {
        User actor = requireActiveAdmin(actorId);
        String normalized = format == null ? "unknown" : format.toLowerCase(Locale.ROOT);
        saveAudit(actor, "REPORT_EXPORTED", "report", actor.getUserId(), null,
                "{\"reportType\":\"sales\",\"format\":" + json(normalized)
                        + ",\"from\":" + json(report.getFrom().toString())
                        + ",\"to\":" + json(report.getTo().toString())
                        + ",\"generatedAt\":" + json(LocalDateTime.now().toString()) + "}");
    }

    private User requireActiveAdmin(Integer actorId) {
        User actor = users.findById(actorId)
                .orElseThrow(() -> new IllegalArgumentException("Administrator account was not found."));
        if (!actor.isActive() || actor.getRole() != User.Role.admin) {
            throw new IllegalStateException("Administrator permission is required.");
        }
        return actor;
    }

    private void saveAudit(User actor, String action, String table, Integer id, String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setActionType(action);
        log.setTableName(table);
        log.setRecordId(id == null ? actor.getUserId() : id);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        audits.save(log);
    }

    private String csvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        String safe = value;
        if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private String prettyAndRedact(String raw) {
        if (raw == null || raw.isBlank()) {
            return "No value recorded.";
        }
        String redacted = BCRYPT.matcher(raw).replaceAll("[REDACTED_BCRYPT]");
        Matcher matcher = SENSITIVE_JSON_VALUE.matcher(redacted);
        redacted = matcher.replaceAll("$1\"***REDACTED***\"");
        return prettyJsonLike(redacted);
    }

    private String prettyJsonLike(String value) {
        String trimmed = value.trim();
        if (!(trimmed.startsWith("{") || trimmed.startsWith("["))) {
            return trimmed;
        }
        StringBuilder out = new StringBuilder();
        int indent = 0;
        boolean inString = false;
        boolean escaped = false;
        for (char c : trimmed.toCharArray()) {
            if (inString) {
                out.append(c);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                out.append(c);
            } else if (c == '{' || c == '[') {
                out.append(c).append('\n');
                indent++;
                appendIndent(out, indent);
            } else if (c == '}' || c == ']') {
                out.append('\n');
                indent = Math.max(0, indent - 1);
                appendIndent(out, indent);
                out.append(c);
            } else if (c == ',') {
                out.append(c).append('\n');
                appendIndent(out, indent);
            } else if (c == ':') {
                out.append(": ");
            } else if (!Character.isWhitespace(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    private void appendIndent(StringBuilder out, int indent) {
        out.append("  ".repeat(Math.max(0, indent)));
    }

    private String json(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String blank(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String money(BigDecimal value) {
        return zero(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String trimPdf(String value, int max) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= max ? clean : clean.substring(0, Math.max(0, max - 3)) + "...";
    }
}
