package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.AuditLog;
import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.PurchaseOrder;
import com.sliit.sparepartshub.entity.PurchaseOrderItem;
import com.sliit.sparepartshub.entity.RestockSuggestion;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.entity.User;
import com.sliit.sparepartshub.repository.AuditLogRepository;
import com.sliit.sparepartshub.stockmonitoring.dto.UrgencyRow;
import com.sliit.sparepartshub.stockmonitoring.repository.MonitoringProductRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.MonitoringPurchaseOrderItemRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.MonitoringSaleItemRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.MonitoringStockRequestRepository;
import com.sliit.sparepartshub.stockmonitoring.repository.RestockSuggestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class StockMonitoringService {

    /**
     * Business policy used by Function 3. See docs/urgency-formula.md.
     */
    private static final BigDecimal CRITICAL_THRESHOLD = BigDecimal.valueOf(70);
    private static final BigDecimal WARNING_THRESHOLD = BigDecimal.valueOf(40);
    private static final BigDecimal SUGGESTION_THRESHOLD = WARNING_THRESHOLD;
    private static final int SALES_WINDOW_DAYS = 30;
    private static final int COVERAGE_TARGET_DAYS = 14;

    private final MonitoringProductRepository products;
    private final MonitoringSaleItemRepository sales;
    private final MonitoringStockRequestRepository requests;
    private final MonitoringPurchaseOrderItemRepository poItems;
    private final RestockSuggestionRepository suggestions;
    private final AuditLogRepository audits;

    private volatile LocalDateTime lastRecalculatedAt;

    public StockMonitoringService(MonitoringProductRepository products,
                                  MonitoringSaleItemRepository sales,
                                  MonitoringStockRequestRepository requests,
                                  MonitoringPurchaseOrderItemRepository poItems,
                                  RestockSuggestionRepository suggestions,
                                  AuditLogRepository audits) {
        this.products = products;
        this.sales = sales;
        this.requests = requests;
        this.poItems = poItems;
        this.suggestions = suggestions;
        this.audits = audits;
    }

    /**
     * Recalculate all products without creating an audit row. This overload is
     * intentionally kept because Sales/POS calls it after a successful checkout
     * and the scheduler also uses it.
     */
    @Transactional
    public List<UrgencyRow> recalculateAll() {
        List<UrgencyRow> rows = recalculateAllInternal();
        lastRecalculatedAt = LocalDateTime.now();
        return rows;
    }

    /**
     * Manual recalculation initiated by a staff user. The calculation is the
     * same as the scheduled/event-driven path; only this path writes the
     * URGENCY_RECALCULATED audit event required by Function 3.
     */
    @Transactional
    public List<UrgencyRow> recalculateAll(User actor) {
        if (actor == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }

        List<UrgencyRow> rows = recalculateAllInternal();
        lastRecalculatedAt = LocalDateTime.now();

        audit(
                actor,
                "URGENCY_RECALCULATED",
                "stock_monitoring",
                actor.getUserId(),
                null,
                "{\"productCount\":" + rows.size()
                        + ",\"recalculatedAt\":" + jsonString(lastRecalculatedAt.toString()) + "}"
        );

        return rows;
    }

    /**
     * Used after a focused event such as receiving stock for one product.
     */
    @Transactional
    public UrgencyRow recalculateProduct(Integer productId) {
        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        UrgencyRow row = calculateAndPersist(product);
        lastRecalculatedAt = LocalDateTime.now();
        return row;
    }

    /**
     * Read-only dashboard representation. It keeps the persisted score but
     * recalculates the explanatory metrics from live data so users can see why
     * the current score exists without turning every GET request into a write.
     */
    @Transactional(readOnly = true)
    public List<UrgencyRow> dashboardRows() {
        List<UrgencyRow> rows = new ArrayList<>();
        for (Product product : products.findAllByOrderByUrgencyScoreDesc()) {
            Metrics metrics = metricsFor(product);
            rows.add(toRow(product, metrics));
        }
        return rows;
    }

    public LocalDateTime getLastRecalculatedAt() {
        return lastRecalculatedAt;
    }

    @Transactional(readOnly = true)
    public List<RestockSuggestion> suggestions() {
        return suggestions.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<StockRequest> stockRequests() {
        return requests.findAllByOrderByRequestedAtDesc();
    }

    /**
     * Shared dashboard uses this value as the number of unresolved customer
     * requests, not only rows whose literal status is "pending".
     */
    public long pendingRequestCount() {
        return requests.countByStatusNot(StockRequest.Status.fulfilled);
    }

    public long readyToNotifyCount() {
        return requests.countByStatus(StockRequest.Status.ready_to_notify);
    }

    @Transactional
    public void decide(Integer id,
                       RestockSuggestion.Status status,
                       Integer quantity,
                       String reason,
                       User actor) {
        if (actor == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }
        if (status == null || status == RestockSuggestion.Status.pending) {
            throw new IllegalArgumentException("Choose approve, modify or reject.");
        }

        RestockSuggestion suggestion = suggestions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Suggestion not found."));

        if (suggestion.getStatus() != RestockSuggestion.Status.pending) {
            throw new IllegalArgumentException("This suggestion has already been reviewed.");
        }

        int oldQuantity = suggestion.getSuggestedQuantity();
        String oldReason = suggestion.getReason();

        String cleanReason = cleanOptional(reason, 255);

        if (status == RestockSuggestion.Status.modified) {
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException("Modified quantity must be positive.");
            }
            if (cleanReason == null) {
                throw new IllegalArgumentException("A modification reason is required.");
            }
            suggestion.setSuggestedQuantity(quantity);
        }

        if (status == RestockSuggestion.Status.rejected && cleanReason == null) {
            throw new IllegalArgumentException("A rejection reason is required.");
        }

        suggestion.setStatus(status);
        if (cleanReason != null) {
            suggestion.setReason(cleanReason);
        }
        suggestion.setReviewedBy(actor);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestions.save(suggestion);

        audit(
                actor,
                switch (status) {
                    case approved -> "RESTOCK_APPROVED";
                    case modified -> "RESTOCK_MODIFIED";
                    case rejected -> "RESTOCK_REJECTED";
                    default -> "RESTOCK_REVIEWED";
                },
                "restock_suggestion",
                suggestion.getSuggestionId(),
                "{\"status\":\"pending\",\"quantity\":" + oldQuantity
                        + ",\"reason\":" + jsonString(oldReason) + "}",
                "{\"status\":" + jsonString(status.name())
                        + ",\"quantity\":" + suggestion.getSuggestedQuantity()
                        + ",\"reason\":" + jsonString(suggestion.getReason()) + "}"
        );
    }

    @Transactional
    public StockRequest createRequest(Integer productId,
                                      Integer quantity,
                                      String customerName,
                                      String customerEmail,
                                      String customerPhone,
                                      User actor) {
        if (actor == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }

        Product product = products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Requested quantity must be positive.");
        }

        String cleanName = required(customerName, "Customer name", 50);
        String cleanEmail = cleanOptional(customerEmail, 254);
        String cleanPhone = cleanOptional(customerPhone, 20);

        if (cleanEmail == null && cleanPhone == null) {
            throw new IllegalArgumentException("Provide at least an email address or phone number.");
        }

        StockRequest request = new StockRequest();
        request.setProduct(product);
        request.setLoggedBy(actor);
        request.setRequestedQuantity(quantity);
        request.setCustomerName(cleanName);
        request.setCustomerEmail(cleanEmail);
        request.setCustomerPhonenum(cleanPhone);

        int alreadyReadyDemand = safeLongToInt(requests.demandByStatuses(
                product.getProductId(),
                List.of(StockRequest.Status.ready_to_notify, StockRequest.Status.notified)
        ));
        int currentlyAvailableForRequests = Math.max(0, safeStock(product) - alreadyReadyDemand);

        if (currentlyAvailableForRequests >= quantity) {
            request.setStatus(StockRequest.Status.ready_to_notify);
        } else {
            request.setStatus(StockRequest.Status.pending);
        }

        request = requests.save(request);

        audit(
                actor,
                "STOCK_REQUEST_CREATED",
                "stock_request",
                request.getRequestId(),
                null,
                "{\"productId\":" + product.getProductId()
                        + ",\"quantity\":" + quantity
                        + ",\"status\":" + jsonString(request.getStatus().name()) + "}"
        );

        recalculateProduct(product.getProductId());
        return request;
    }

    @Transactional
    public void markRequest(Integer id, StockRequest.Status targetStatus, User actor) {
        if (actor == null) {
            throw new IllegalArgumentException("Authenticated staff user is required.");
        }
        if (targetStatus == null) {
            throw new IllegalArgumentException("Request status is required.");
        }

        StockRequest request = requests.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stock request not found."));

        StockRequest.Status oldStatus = request.getStatus();
        if (oldStatus == targetStatus) {
            return;
        }

        validateRequestTransition(request, targetStatus);

        request.setStatus(targetStatus);
        if (targetStatus == StockRequest.Status.notified) {
            request.setNotifiedAt(LocalDateTime.now());
        }
        requests.save(request);

        audit(
                actor,
                "STOCK_REQUEST_STATUS",
                "stock_request",
                request.getRequestId(),
                "{\"status\":" + jsonString(oldStatus.name()) + "}",
                "{\"status\":" + jsonString(targetStatus.name()) + "}"
        );

        recalculateProduct(request.getProduct().getProductId());
    }

    @Transactional(readOnly = true)
    public List<Product> products() {
        return products.findAllByOrderByUrgencyScoreDesc();
    }

    public String classificationFor(BigDecimal score) {
        BigDecimal safeScore = score == null ? BigDecimal.ZERO : score;
        if (safeScore.compareTo(CRITICAL_THRESHOLD) >= 0) {
            return "Critical";
        }
        if (safeScore.compareTo(WARNING_THRESHOLD) >= 0) {
            return "Warning";
        }
        return "Safe";
    }

    private List<UrgencyRow> recalculateAllInternal() {
        List<UrgencyRow> rows = new ArrayList<>();
        for (Product product : products.findAll()) {
            rows.add(calculateAndPersist(product));
        }
        rows.sort(Comparator.comparing(
                (UrgencyRow row) -> row.getProduct().getUrgencyScore(),
                Comparator.nullsFirst(Comparator.naturalOrder())
        ).reversed());
        return rows;
    }

    private UrgencyRow calculateAndPersist(Product product) {
        Metrics metrics = metricsFor(product);
        BigDecimal score = calculateScore(product, metrics);
        product.setUrgencyScore(score);
        products.save(product);

        int suggestedQuantity = suggestedQuantity(product, metrics);
        ensureSuggestion(product, score, suggestedQuantity);

        return new UrgencyRow(
                product,
                metrics.sold30,
                metrics.dailySalesVelocity,
                metrics.openDemand,
                metrics.incoming,
                suggestedQuantity,
                classificationFor(score)
        );
    }

    private UrgencyRow toRow(Product product, Metrics metrics) {
        return new UrgencyRow(
                product,
                metrics.sold30,
                metrics.dailySalesVelocity,
                metrics.openDemand,
                metrics.incoming,
                suggestedQuantity(product, metrics),
                classificationFor(product.getUrgencyScore())
        );
    }

    private Metrics metricsFor(Product product) {
        LocalDateTime since = LocalDateTime.now().minusDays(SALES_WINDOW_DAYS);
        long sold30 = safeLong(sales.sold(product.getProductId(), since));
        long openDemand = safeLong(requests.openDemand(
                product.getProductId(),
                StockRequest.Status.fulfilled
        ));
        long incoming = incomingQuantity(product.getProductId());

        BigDecimal dailyVelocity = BigDecimal.valueOf(sold30)
                .divide(BigDecimal.valueOf(SALES_WINDOW_DAYS), 2, RoundingMode.HALF_UP);

        return new Metrics(sold30, dailyVelocity, openDemand, incoming);
    }

    private long incomingQuantity(Integer productId) {
        return poItems.findByProduct_ProductId(productId).stream()
                .filter(item -> isOpenPurchaseOrder(item.getPurchaseOrder()))
                .mapToLong(item -> Math.max(
                        0,
                        safeInt(item.getQuantityOrdered()) - item.getReceivedQuantity()
                ))
                .sum();
    }

    private boolean isOpenPurchaseOrder(PurchaseOrder po) {
        return po != null
                && (po.getStatus() == PurchaseOrder.Status.pending
                || po.getStatus() == PurchaseOrder.Status.shipped
                || po.getStatus() == PurchaseOrder.Status.partially_received);
    }

    /**
     * Formula (0-100):
     *  target stock       reorder level + 14 days of recent sales + open demand
     *  shortage pressure  0..60  based on shortage relative to target stock
     *  sales pressure     0..20  30-day sales relative to reorder level
     *  demand pressure    0..20  unresolved customer demand relative to reorder level
     *  stock-out boost    0..10
     *  incoming relief    0..30  remaining open PO quantity relative to target stock
     *
     * This makes incoming POs reduce urgency without hiding real customer demand,
     * and makes an actual stock-out critical even when historical sales are low.
     */
    private BigDecimal calculateScore(Product product, Metrics metrics) {
        double reorder = Math.max(1, safeInt(product.getReorderLevel()));
        double stock = Math.max(0, safeStock(product));
        double twoWeekSales = Math.ceil(metrics.dailySalesVelocity.doubleValue() * COVERAGE_TARGET_DAYS);
        double targetStock = Math.max(1.0, reorder + twoWeekSales + metrics.openDemand);
        double shortage = Math.max(0.0, targetStock - stock);

        double shortagePressure = clamp((shortage / targetStock) * 60.0, 0, 60);
        double salesPressure = clamp((metrics.sold30 / reorder) * 20.0, 0, 20);
        double demandPressure = clamp((metrics.openDemand / reorder) * 20.0, 0, 20);
        double stockOutBoost = stock <= 0 ? 10.0 : 0.0;
        double incomingRelief = clamp((metrics.incoming / targetStock) * 30.0, 0, 30);

        double score = clamp(
                shortagePressure + salesPressure + demandPressure + stockOutBoost - incomingRelief,
                0,
                100
        );

        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Suggested quantity targets the product's reorder level plus roughly two
     * weeks of recent sales and unresolved demand, then subtracts stock already
     * on hand and open incoming PO quantity.
     */
    private int suggestedQuantity(Product product, Metrics metrics) {
        int reorder = Math.max(1, safeInt(product.getReorderLevel()));
        int stock = Math.max(0, safeStock(product));
        int twoWeekSales = (int) Math.ceil(metrics.dailySalesVelocity.doubleValue() * COVERAGE_TARGET_DAYS);

        long target = (long) reorder
                + twoWeekSales
                + metrics.openDemand
                - stock
                - metrics.incoming;

        if (target <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, target);
    }

    private void ensureSuggestion(Product product, BigDecimal score, int suggestedQuantity) {
        if (score.compareTo(SUGGESTION_THRESHOLD) < 0 || suggestedQuantity < 1) {
            return;
        }

        List<RestockSuggestion> history =
                suggestions.findByProduct_ProductIdOrderByCreatedAtDesc(product.getProductId());

        boolean unresolvedExists = history.stream().anyMatch(suggestion ->
                suggestion.getStatus() == RestockSuggestion.Status.pending
                        || suggestion.getStatus() == RestockSuggestion.Status.approved
                        || suggestion.getStatus() == RestockSuggestion.Status.modified
        );

        if (unresolvedExists) {
            return;
        }

        RestockSuggestion suggestion = new RestockSuggestion();
        suggestion.setProduct(product);
        suggestion.setSuggestedQuantity(suggestedQuantity);
        suggestion.setReason(
                "Generated from reorder level, 30-day sales velocity, unresolved customer demand and incoming PO quantity."
        );
        suggestions.save(suggestion);
    }

    private void validateRequestTransition(StockRequest request, StockRequest.Status targetStatus) {
        StockRequest.Status current = request.getStatus();

        switch (current) {
            case pending -> {
                if (targetStatus != StockRequest.Status.ready_to_notify) {
                    throw new IllegalArgumentException(
                            "A pending request must become Ready to Notify before the customer can be marked notified."
                    );
                }
                int committed = safeLongToInt(requests.demandByStatuses(
                        request.getProduct().getProductId(),
                        List.of(StockRequest.Status.ready_to_notify, StockRequest.Status.notified)
                ));
                int available = Math.max(0, safeStock(request.getProduct()) - committed);
                if (available < safeInt(request.getRequestedQuantity())) {
                    throw new IllegalArgumentException(
                            "Enough unallocated stock is not available yet to mark this request Ready to Notify."
                    );
                }
            }
            case ready_to_notify -> {
                if (targetStatus != StockRequest.Status.notified) {
                    throw new IllegalArgumentException(
                            "A Ready to Notify request can only be marked Notified."
                    );
                }
            }
            case notified -> {
                if (targetStatus != StockRequest.Status.fulfilled) {
                    throw new IllegalArgumentException(
                            "A notified request can only be marked Fulfilled."
                    );
                }
            }
            case fulfilled -> throw new IllegalArgumentException("A fulfilled request is already closed.");
        }
    }

    private int safeStock(Product product) {
        return product.getStockCount() == null ? 0 : product.getStockCount();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private int safeLongToInt(Long value) {
        long safe = safeLong(value);
        return safe > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) safe;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String required(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new IllegalArgumentException(label + " must not exceed " + maxLength + " characters.");
        }
        return clean;
    }

    private String cleanOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String clean = value.trim();
        if (clean.length() > maxLength) {
            throw new IllegalArgumentException("Value must not exceed " + maxLength + " characters.");
        }
        return clean;
    }

    private void audit(User actor,
                       String action,
                       String table,
                       Integer recordId,
                       String oldValue,
                       String newValue) {
        AuditLog log = new AuditLog();
        log.setUser(actor);
        log.setActionType(action);
        log.setTableName(table);
        log.setRecordId(recordId == null ? 0 : recordId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        audits.save(log);
    }

    private String jsonString(String value) {
        if (value == null) {
            return "null";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        escaped.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch <= 0x1F) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static final class Metrics {
        private final long sold30;
        private final BigDecimal dailySalesVelocity;
        private final long openDemand;
        private final long incoming;

        private Metrics(long sold30,
                        BigDecimal dailySalesVelocity,
                        long openDemand,
                        long incoming) {
            this.sold30 = sold30;
            this.dailySalesVelocity = dailySalesVelocity;
            this.openDemand = openDemand;
            this.incoming = incoming;
        }
    }
}
