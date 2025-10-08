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
 * Consumes data export API from Symfony backend
 */
@Service
public class SymfonyApiClient {

    private static final Logger log = LoggerFactory.getLogger(SymfonyApiClient.class);

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiBaseUrl;

    @Value("${symfony.api.timeout:30}")
    private int timeoutSeconds;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SymfonyApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetch all orders from Symfony API
     */
    public List<Map<String, Object>> fetchAllOrders() {
        return fetchAllPaginated("/api/export/orders");
    }

    /**
     * Fetch orders by status
     */
    public List<Map<String, Object>> fetchOrdersByStatus(int status) {
        return fetchAllPaginated("/api/export/orders?status=" + status);
    }

    /**
     * Fetch orders modified since a specific date (incremental sync)
     */
    public List<Map<String, Object>> fetchOrdersSince(String since) {
        return fetchAllPaginated("/api/export/orders?since=" + since);
    }

    /**
     * Fetch all cards from Symfony API
     */
    public List<Map<String, Object>> fetchAllCards() {
        return fetchAllPaginated("/api/export/cards");
    }

    /**
     * Fetch all card translations from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardTranslations() {
        return fetchAllPaginated("/api/export/card-translations");
    }

    /**
     * Fetch all card certifications from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardCertifications() {
        return fetchAllPaginated("/api/export/card-certifications");
    }

    /**
     * Fetch all card certification orders from Symfony API
     */
    public List<Map<String, Object>> fetchAllCardCertificationOrders() {
        return fetchAllPaginated("/api/export/card-certification-orders");
    }

    /**
     * Get statistics from Symfony API
     */
    public Map<String, Object> getStats() {
        try {
            String url = symfonyApiBaseUrl + "/api/export/stats";
            log.info("📊 Fetching stats from: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());

                if (root.get("success").asBoolean()) {
                    return objectMapper.convertValue(root.get("data"), Map.class);
                } else {
                    throw new RuntimeException("API returned success=false");
                }
            } else {
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            }

        } catch (Exception e) {
            log.error("❌ Error fetching stats from Symfony API", e);
            throw new RuntimeException("Failed to fetch stats: " + e.getMessage(), e);
        }
    }

    /**
     * Check Symfony API health
     */
    public boolean isHealthy() {
        try {
            String url = symfonyApiBaseUrl + "/api/export/health";
            log.debug("🏥 Checking Symfony API health: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.get("success").asBoolean();
            }

            return false;

        } catch (Exception e) {
            log.warn("⚠️ Symfony API health check failed", e);
            return false;
        }
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Fetch all pages from a paginated endpoint
     */
    private List<Map<String, Object>> fetchAllPaginated(String endpoint) {
        List<Map<String, Object>> allData = new ArrayList<>();
        int offset = 0;
        int limit = 1000;
        boolean hasMore = true;

        try {
            while (hasMore) {
                String url = symfonyApiBaseUrl + endpoint;

                // Add pagination params
                if (endpoint.contains("?")) {
                    url += "&limit=" + limit + "&offset=" + offset;
                } else {
                    url += "?limit=" + limit + "&offset=" + offset;
                }

                log.debug("🔄 Fetching: {} (offset: {})", endpoint, offset);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());

                    if (root.get("success").asBoolean()) {
                        JsonNode dataNode = root.get("data");
                        List<Map<String, Object>> pageData = objectMapper.convertValue(dataNode, List.class);
                        allData.addAll(pageData);

                        // Check if there's more data
                        JsonNode pagination = root.get("pagination");
                        hasMore = pagination.get("hasMore").asBoolean();
                        offset += limit;

                        log.debug("✅ Fetched {} records, hasMore: {}", pageData.size(), hasMore);
                    } else {
                        throw new RuntimeException("API returned success=false");
                    }
                } else {
                    throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
                }
            }

            log.info("✅ Successfully fetched {} total records from {}", allData.size(), endpoint);
            return allData;

        } catch (Exception e) {
            log.error("❌ Error fetching data from Symfony API: {}", endpoint, e);
            throw new RuntimeException("Failed to fetch data from " + endpoint + ": " + e.getMessage(), e);
        }
    }
}