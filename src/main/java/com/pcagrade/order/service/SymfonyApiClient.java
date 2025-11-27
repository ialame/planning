package com.pcagrade.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Symfony API Client
 * UPDATED: Uses new GptOrderController API (/gpt/orders)
 *
 * Consumes data export API from Symfony backend for planning synchronization
 */
@Service
public class SymfonyApiClient {

    private static final Logger log = LoggerFactory.getLogger(SymfonyApiClient.class);

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiBaseUrl;

    @Value("${symfony.api.key:}")
    private String symfonyApiKey;

    @Value("${symfony.api.timeout:60}")
    private int timeoutSeconds;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SymfonyApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    // ============================================================
    // NEW API METHODS (GptOrderController)
    // ============================================================

    /**
     * Fetch all active orders from new GptOrderController API
     * Endpoint: GET /gpt/orders
     */
    public List<Map<String, Object>> fetchAllOrders() {
        return fetchAllOrdersPaginated(500);
    }

    /**
     * Fetch orders with pagination from new API
     */
    public List<Map<String, Object>> fetchAllOrdersPaginated(int pageSize) {
        List<Map<String, Object>> allOrders = new ArrayList<>();
        int offset = 0;
        boolean hasMore = true;

        while (hasMore) {
            try {
                String url = String.format("%s/gpt/orders?limit=%d&offset=%d",
                        symfonyApiBaseUrl, pageSize, offset);

                log.debug(" Fetching orders from: {}", url);

                HttpRequest request = buildAuthenticatedRequest(url);

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.error(" API error: {} - {}", response.statusCode(), response.body());
                    break;
                }

                JsonNode root = objectMapper.readTree(response.body());

                // Check success flag
                if (!root.path("success").asBoolean(false)) {
                    log.error(" API returned success=false");
                    break;
                }

                // Get orders from "data" array (new structure)
                JsonNode dataNode = root.path("data");
                if (dataNode.isArray()) {
                    for (JsonNode orderNode : dataNode) {
                        Map<String, Object> order = objectMapper.convertValue(orderNode, Map.class);
                        allOrders.add(order);
                    }
                }

                // Check if there are more pages
                JsonNode metaNode = root.path("meta");
                int total = metaNode.path("total").asInt(0);
                int currentBatchSize = dataNode.size();

                log.debug(" Fetched {} orders (total: {}, offset: {})",
                        currentBatchSize, total, offset);

                offset += pageSize;
                hasMore = currentBatchSize == pageSize && offset < total;

            } catch (Exception e) {
                log.error(" Error fetching orders at offset {}: {}", offset, e.getMessage());
                break;
            }
        }

        log.info(" Total orders fetched: {}", allOrders.size());
        return allOrders;
    }

    /**
     * Fetch a single order by order number
     * Endpoint: GET /gpt/orders/{orderNumber}
     */
    public Map<String, Object> fetchOrderByNumber(String orderNumber) {
        try {
            String url = String.format("%s/gpt/orders/%s", symfonyApiBaseUrl, orderNumber);

            HttpRequest request = buildAuthenticatedRequest(url);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error(" Order not found: {}", orderNumber);
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("success").asBoolean(false)) {
                return null;
            }

            return objectMapper.convertValue(root.path("data"), Map.class);

        } catch (Exception e) {
            log.error(" Error fetching order {}: {}", orderNumber, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch order history
     * Endpoint: GET /gpt/orders/{orderNumber}/history
     */
    public List<Map<String, Object>> fetchOrderHistory(String orderNumber) {
        try {
            String url = String.format("%s/gpt/orders/%s/history",
                    symfonyApiBaseUrl, orderNumber);

            HttpRequest request = buildAuthenticatedRequest(url);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("success").asBoolean(false)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> history = new ArrayList<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    history.add(objectMapper.convertValue(node, Map.class));
                }
            }

            return history;

        } catch (Exception e) {
            log.error(" Error fetching history for order {}: {}", orderNumber, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch order date history
     * Endpoint: GET /gpt/orders/{orderNumber}/date-history
     */
    public List<Map<String, Object>> fetchOrderDateHistory(String orderNumber) {
        try {
            String url = String.format("%s/gpt/orders/%s/date-history",
                    symfonyApiBaseUrl, orderNumber);

            HttpRequest request = buildAuthenticatedRequest(url);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("success").asBoolean(false)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> dateHistory = new ArrayList<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    dateHistory.add(objectMapper.convertValue(node, Map.class));
                }
            }

            return dateHistory;

        } catch (Exception e) {
            log.error(" Error fetching date history for order {}: {}", orderNumber, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch latest histories
     * Endpoint: GET /gpt/histories
     */
    public List<Map<String, Object>> fetchLatestHistories(int limit, boolean activeOrdersOnly) {
        try {
            String url = String.format("%s/gpt/histories?limit=%d&active_orders_only=%s",
                    symfonyApiBaseUrl, limit, activeOrdersOnly);

            HttpRequest request = buildAuthenticatedRequest(url);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("success").asBoolean(false)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> histories = new ArrayList<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    histories.add(objectMapper.convertValue(node, Map.class));
                }
            }

            return histories;

        } catch (Exception e) {
            log.error(" Error fetching histories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Fetch latest date histories
     * Endpoint: GET /gpt/date-histories
     */
    public List<Map<String, Object>> fetchLatestDateHistories(int limit) {
        try {
            String url = String.format("%s/gpt/date-histories?limit=%d",
                    symfonyApiBaseUrl, limit);

            HttpRequest request = buildAuthenticatedRequest(url);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return new ArrayList<>();
            }

            JsonNode root = objectMapper.readTree(response.body());

            if (!root.path("success").asBoolean(false)) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> histories = new ArrayList<>();
            JsonNode dataNode = root.path("data");
            if (dataNode.isArray()) {
                for (JsonNode node : dataNode) {
                    histories.add(objectMapper.convertValue(node, Map.class));
                }
            }

            return histories;

        } catch (Exception e) {
            log.error(" Error fetching date histories: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================
    // HEALTH CHECK
    // ============================================================

    /**
     * Check if the Symfony API is accessible
     */
    public boolean isApiHealthy() {
        try {
            String url = String.format("%s/gpt/orders?limit=1", symfonyApiBaseUrl);

            HttpRequest request = buildAuthenticatedRequest(url, 10);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("success").asBoolean(false);
            }

            return false;

        } catch (Exception e) {
            log.warn(" API health check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get API statistics
     */
    public Map<String, Object> getApiStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Get total count from API
            String url = String.format("%s/gpt/orders?limit=1&offset=0", symfonyApiBaseUrl);

            HttpRequest request = buildAuthenticatedRequest(url, 10);

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode meta = root.path("meta");

                stats.put("total_orders", meta.path("total").asInt(0));
                stats.put("api_healthy", true);
                stats.put("api_url", symfonyApiBaseUrl);
                stats.put("endpoint", "/gpt/orders");
            } else {
                stats.put("api_healthy", false);
                stats.put("error", "HTTP " + response.statusCode());
            }

        } catch (Exception e) {
            stats.put("api_healthy", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // ============================================================
    // GETTERS/SETTERS
    // ============================================================

    public String getSymfonyApiBaseUrl() {
        return symfonyApiBaseUrl;
    }

    public void setSymfonyApiBaseUrl(String url) {
        this.symfonyApiBaseUrl = url;
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    /**
     * Build an authenticated HTTP request with default timeout
     */
    private HttpRequest buildAuthenticatedRequest(String url) {
        return buildAuthenticatedRequest(url, timeoutSeconds);
    }

    /**
     * Build an authenticated HTTP request with custom timeout
     */
    private HttpRequest buildAuthenticatedRequest(String url, int timeout) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeout))
                .GET();

        // Add Authorization header if API key is configured
        if (symfonyApiKey != null && !symfonyApiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + symfonyApiKey);
        }

        return builder.build();
    }
}
