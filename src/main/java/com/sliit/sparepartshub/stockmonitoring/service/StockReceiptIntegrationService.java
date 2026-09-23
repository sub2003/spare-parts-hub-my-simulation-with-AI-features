package com.sliit.sparepartshub.stockmonitoring.service;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.entity.StockRequest;
import com.sliit.sparepartshub.stockmonitoring.repository.MonitoringStockRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Integration hook called by Supplier Management after accepted PO stock has
 * already been added to Product.stockCount. It deliberately does NOT alter
 * stock itself, preventing a second inventory addition.
 */
@Service
public class StockReceiptIntegrationService {

    private final MonitoringStockRequestRepository requests;
    private final StockMonitoringService monitoring;

    public StockReceiptIntegrationService(MonitoringStockRequestRepository requests,
                                          StockMonitoringService monitoring) {
        this.requests = requests;
        this.monitoring = monitoring;
    }

    @Transactional
    public void onStockReceived(Product product) {
        if (product == null || product.getProductId() == null) {
            return;
        }

        int stock = Math.max(0, product.getStockCount() == null ? 0 : product.getStockCount());
        long alreadyReadyDemand = requests.demandByStatuses(
                product.getProductId(),
                List.of(StockRequest.Status.ready_to_notify, StockRequest.Status.notified)
        );
        int committedForFollowUp = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, alreadyReadyDemand));
        int available = Math.max(0, stock - committedForFollowUp);

        List<StockRequest> pending = requests
                .findByProduct_ProductIdAndStatusOrderByRequestedAtAsc(
                        product.getProductId(),
                        StockRequest.Status.pending
                );

        /*
         * Requests are considered FIFO for notification readiness. This is not
         * a stock reservation: it only prevents marking more customer demand as
         * "ready" than the currently available units can satisfy.
         */
        for (StockRequest request : pending) {
            int requested = Math.max(1,
                    request.getRequestedQuantity() == null ? 1 : request.getRequestedQuantity());

            if (requested > available) {
                break;
            }

            request.setStatus(StockRequest.Status.ready_to_notify);
            requests.save(request);
            available -= requested;
        }

        monitoring.recalculateProduct(product.getProductId());
    }
}
