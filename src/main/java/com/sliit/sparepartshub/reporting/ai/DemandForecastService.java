package com.sliit.sparepartshub.reporting.ai;

import com.sliit.sparepartshub.entity.Product;
import com.sliit.sparepartshub.reporting.repository.ReportingProductRepository;
import com.sliit.sparepartshub.reporting.repository.ReportingSaleItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DemandForecastService {

    private final ReportingProductRepository products;
    private final ReportingSaleItemRepository saleItems;
    private final boolean enabled;
    private final RestClient client;

    public DemandForecastService(ReportingProductRepository products,
                                 ReportingSaleItemRepository saleItems,
                                 @Value("${ai.forecast.enabled:true}") boolean enabled,
                                 @Value("${ai.forecast.base-url:http://127.0.0.1:8000}") String baseUrl,
                                 @Value("${ai.forecast.timeout-ms:1800}") int timeoutMs) {
        this.products = products;
        this.saleItems = saleItems;
        this.enabled = enabled;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(250, timeoutMs)))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(250, timeoutMs)));
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public AiForecastDashboard dashboard() {
        if (!enabled) {
            return AiForecastDashboard.unavailable("AI forecasting is disabled in application.properties.");
        }
        try {
            AiModelStatus status = client.get()
                    .uri("/model/status")
                    .retrieve()
                    .body(AiModelStatus.class);
            if (status == null || status.version() == null) {
                String state = status == null ? "UNAVAILABLE" : status.status();
                return AiForecastDashboard.unavailable("AI forecasting has no active model yet (" + state + ").");
            }

            RawForecast[] raw = client.get()
                    .uri("/forecast/products")
                    .retrieve()
                    .body(RawForecast[].class);
            if (raw == null) {
                raw = new RawForecast[0];
            }

            Map<Integer, Product> currentProducts = new HashMap<>();
            for (Product product : products.findAll()) {
                currentProducts.put(product.getProductId(), product);
            }

            List<AiForecastRow> rows = new ArrayList<>();
            for (RawForecast forecast : raw) {
                Product product = currentProducts.get(forecast.productId());
                if (product == null) {
                    continue; // stale model catalogue entry; never invent a current product
                }
                int currentStock = Math.max(0, Objects.requireNonNullElse(product.getStockCount(), 0));
                int forecast30 = Math.max(0, forecast.forecast30Days());
                int shortage = Math.max(0, forecast30 - currentStock);
                String historyCsv = forecast.history30() == null ? "" : forecast.history30().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(","));
                rows.add(new AiForecastRow(
                        product.getProductId(),
                        product.getProductCode(),
                        product.getName(),
                        product.getCategory(),
                        Math.max(0, forecast.forecast7Days()),
                        forecast30,
                        forecast.trend(),
                        forecast.scope(),
                        currentStock,
                        shortage,
                        historyCsv,
                        Math.max(0.0, forecast.forecast30DailyAverage())
                ));
            }
            rows.sort(Comparator.comparingInt(AiForecastRow::forecast30Days).reversed()
                    .thenComparing(AiForecastRow::productCode));
            return AiForecastDashboard.available(status, rows.stream().limit(10).toList());
        } catch (RuntimeException ex) {
            return AiForecastDashboard.unavailable(
                    "AI forecasting temporarily unavailable. Start the local FastAPI service on 127.0.0.1:8000.");
        }
    }

    public String requestRetraining(boolean force) {
        if (!enabled) {
            throw new IllegalStateException("AI forecasting is disabled.");
        }

        // Build the retraining payload with explicit JSON keys.
        // This avoids serializer-specific record naming/visibility differences at the
        // Spring Boot -> FastAPI boundary and keeps the contract identical to the
        // Pydantic schema used by the AI service.
        List<Map<String, Object>> catalog = products.findAll().stream()
                .map(p -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("productId", p.getProductId());
                    item.put("productCode", p.getProductCode());
                    item.put("name", p.getName());
                    item.put("category", p.getCategory());
                    item.put("brand", p.getBrand());
                    return item;
                })
                .toList();

        List<Map<String, Object>> realSales = saleItems.dailyProductSales().stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", row.getSaleDate().toString());
                    item.put("productId", row.getProductId());
                    item.put("unitsSold", Math.toIntExact(row.getUnitsSold()));
                    return item;
                })
                .toList();

        Map<String, Object> retrainPayload = new LinkedHashMap<>();
        retrainPayload.put("products", catalog);
        retrainPayload.put("realSales", realSales);
        retrainPayload.put("force", force);

        RetrainResponse response;
        try {
            response = client.post()
                    .uri("/model/retrain")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(retrainPayload)
                    .retrieve()
                    .body(RetrainResponse.class);
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "AI retraining request failed (HTTP " + ex.getStatusCode().value() + "): "
                            + ex.getResponseBodyAsString(),
                    ex);
        }

        if (response == null) {
            throw new IllegalStateException("AI service did not return a retraining response.");
        }
        return response.message() == null
                ? (response.accepted() ? "AI retraining started." : "AI retraining is already in progress.")
                : response.message();
    }

    private record RawForecast(
            Integer productId,
            String productCode,
            String productName,
            String category,
            int forecast7Days,
            int forecast30Days,
            String trend,
            String scope,
            List<Integer> history30,
            double forecast30DailyAverage,
            String modelVersion,
            String trainedAt,
            String dataSource
    ) {
    }


    private record RetrainResponse(boolean accepted, String status, String message) {
    }
}
