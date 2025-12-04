package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.entity.OrderStatus;
import com.pcagrade.order.model.SyncProgress;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.service.CardCertificationSyncService;
import com.pcagrade.order.service.SyncProgressPublisher;
import com.pcagrade.order.service.TranslationSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Sync Controller with SSE Progress Support
 * UPDATED: Uses new GptOrderController API (/gpt/orders)
 */
@RestController
@RequestMapping("/api/sync")
public class MinimalSyncController {

    private static final Logger log = LoggerFactory.getLogger(MinimalSyncController.class);
    private static final int CARD_BATCH_SIZE = 100;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderRepository orderRepository;
    private final CardCertificationSyncService cardSyncService;
    private final TranslationSyncService translationSyncService;
    private final SyncProgressPublisher progressPublisher;
    private final RestTemplate restTemplate;

    @Value("${symfony.api.base-url:http://localhost:8000}")
    private String symfonyApiUrl;

    @Value("${symfony.api.key:}")
    private String symfonyApiKey;

    public MinimalSyncController(
            OrderRepository orderRepository,
            CardCertificationSyncService cardSyncService,
            TranslationSyncService translationSyncService,
            SyncProgressPublisher progressPublisher,
            RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.cardSyncService = cardSyncService;
        this.translationSyncService = translationSyncService;
        this.progressPublisher = progressPublisher;
        this.restTemplate = restTemplate;
    }

    /**
     * Sync all data: orders then cards
     * POST /api/sync/all?syncId=xxx
     */
    @PostMapping("/all")
    public ResponseEntity<Map<String, Object>> syncAll(
            @RequestParam(required = false) String syncId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        log.info(" Starting complete synchronization with syncId: {}", syncId);

        Map<String, Object> response = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "ALL", "Starting full synchronization..."));

            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "ALL", "Fetching orders from Symfony API..."));

            ResponseEntity<Map<String, Object>> ordersResult = syncOrdersWithProgress(syncId);
            response.put("orders", ordersResult.getBody());

            progressPublisher.publishProgress(syncId,
                    SyncProgress.fetching(syncId, "ALL", "Fetching cards from Symfony API..."));

            ResponseEntity<Map<String, Object>> cardsResult = syncCardsWithProgress(syncId, null, null);
            response.put("cards", cardsResult.getBody());

            long duration = System.currentTimeMillis() - startTime;
            response.put("success", true);
            response.put("duration_ms", duration);
            response.put("message", "Complete sync successful");
            response.put("syncId", syncId);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "ALL",
                            String.format("Synchronization completed in %.1f seconds", duration / 1000.0), 0));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error during complete sync", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync orders from Symfony API (GptOrderController)
     * POST /api/sync/orders?syncId=xxx
     */
    @PostMapping("/orders")
    public ResponseEntity<Map<String, Object>> syncOrders(
            @RequestParam(required = false) String syncId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        return syncOrdersWithProgress(syncId);
    }

    /**
     * Sync orders with progress tracking
     * UPDATED: Uses new /gpt/orders endpoint
     */
    private ResponseEntity<Map<String, Object>> syncOrdersWithProgress(String syncId) {
        log.info(" Starting orders synchronization with syncId: {}", syncId);

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "ORDERS", "Starting orders synchronization..."));

            // NEW API ENDPOINT: /gpt/orders with pagination
            int limit = 500;
            int offset = 0;
            int totalSynced = 0;
            int totalOrders = 0;
            boolean hasMore = true;

            while (hasMore) {
                String ordersUrl = String.format("%s/gpt/orders?limit=%d&offset=%d",
                        symfonyApiUrl, limit, offset);
                log.info(" Fetching orders from: {}", ordersUrl);

                progressPublisher.publishProgress(syncId,
                        SyncProgress.fetching(syncId, "ORDERS",
                                String.format("Fetching orders (offset: %d)...", offset)));

                Map<String, Object> response = getFromSymfonyApi(ordersUrl);

                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("Invalid response from Symfony API: " + response);
                }

                // NEW JSON STRUCTURE: data array instead of orders array
                List<Map<String, Object>> ordersData = (List<Map<String, Object>>) response.get("data");

                // Get total from meta object
                Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                if (meta != null && totalOrders == 0) {
                    totalOrders = getInteger(meta, "total", 0);
                    log.info(" Total orders to sync: {}", totalOrders);
                }

                if (ordersData == null || ordersData.isEmpty()) {
                    hasMore = false;
                    continue;
                }

                log.info(" Received {} orders from Symfony (offset: {})", ordersData.size(), offset);

                // Process orders in batch
                List<Order> ordersToSave = new ArrayList<>();
                for (int i = 0; i < ordersData.size(); i++) {
                    Map<String, Object> orderData = ordersData.get(i);

                    Order order = createOrUpdateOrderFromGptApi(orderData);
                    if (order != null) {
                        ordersToSave.add(order);
                    }

                    // Publish progress every 50 orders
                    if ((totalSynced + i + 1) % 50 == 0) {
                        progressPublisher.publishProgress(syncId,
                                SyncProgress.processing(syncId, "ORDERS",
                                        String.format("Processing order %d/%d", totalSynced + i + 1, totalOrders),
                                        totalSynced + i + 1, totalOrders));
                    }
                }

                // Batch save
                if (!ordersToSave.isEmpty()) {
                    orderRepository.saveAll(ordersToSave);
                    totalSynced += ordersToSave.size();
                    log.info(" Saved batch of {} orders", ordersToSave.size());
                }

                // Check if there are more pages
                offset += limit;
                hasMore = ordersData.size() == limit && offset < totalOrders;
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("synced", totalSynced);
            result.put("total", totalOrders);
            result.put("duration_ms", duration);
            result.put("message", String.format("Synced %d orders in %.1f seconds", totalSynced, duration / 1000.0));
            result.put("syncId", syncId);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "ORDERS",
                            String.format("Orders sync completed: %d orders", totalSynced), totalSynced));

            log.info(" Orders sync completed: {} orders in {}ms", totalSynced, duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Error during orders sync", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Create or update Order from GptOrderController API response
     * NEW: Handles the new JSON structure from /gpt/orders
     */
    private Order createOrUpdateOrderFromGptApi(Map<String, Object> orderData) {
        try {
            // Get Symfony ID (same field name)
            String symfonyId = getString(orderData, "id");
            if (symfonyId == null) {
                log.warn(" Order missing ID, skipping");
                return null;
            }

            // Find existing or create new
            Order order = orderRepository.findBySymfonyOrderId(symfonyId)
                    .orElse(new Order());

            order.setSymfonyOrderId(symfonyId);

            // Order number: same field name
            order.setOrderNumber(getString(orderData, "order_number"));

            // Customer name: NEW STRUCTURE - nested in customer object
            Map<String, Object> customer = (Map<String, Object>) orderData.get("customer");
            if (customer != null) {
                order.setCustomerName(getString(customer, "full_name"));
            } else {
                order.setCustomerName("Unknown");
            }

            // Priority/Delai: NEW FIELD NAME - processing_time instead of delai
            String delai = getString(orderData, "processing_time");
            order.setDelai(delai != null ? delai : "C");

            // Order date: NEW FIELD NAME - date instead of order_date
            String dateStr = getString(orderData, "date");
            order.setDate(parseDateTime(dateStr));

            // Total cards: NEW FIELD NAME - nb_cards instead of total_cards
            order.setTotalCards(getInteger(orderData, "nb_cards", 0));

            // Status: same field name (integer)
            Integer statusCode = getInteger(orderData, "status", 2);
            order.setStatus(convertSymfonyStatusToOrderStatus(statusCode));

            // Price: NEW FIELD NAME - total_amount instead of price
            order.setPrice(getFloat(orderData, "total_amount", 0.0f));

            return order;

        } catch (Exception e) {
            log.error(" Error creating order from GptApi: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sync cards from Symfony API
     * POST /api/sync/cards?syncId=xxx&orderId=xxx
     */
    @PostMapping("/cards")
    public ResponseEntity<Map<String, Object>> syncCards(
            @RequestParam(required = false) String syncId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String symfonyOrderId) {

        if (syncId == null || syncId.isEmpty()) {
            syncId = UUID.randomUUID().toString();
        }

        return syncCardsWithProgress(syncId, orderId, symfonyOrderId);
    }

    /**
     * Sync cards with progress tracking
     * Uses card_certifications from order data
     */
    private ResponseEntity<Map<String, Object>> syncCardsWithProgress(
            String syncId, String orderId, String symfonyOrderId) {

        log.info(" Starting cards synchronization with syncId: {}", syncId);

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            progressPublisher.publishProgress(syncId,
                    SyncProgress.starting(syncId, "CARDS", "Starting cards synchronization..."));

            // Fetch orders with their card certifications
            int limit = 100;
            int offset = 0;
            int totalCardsSynced = 0;
            boolean hasMore = true;

            while (hasMore) {
                String url = String.format("%s/gpt/orders?limit=%d&offset=%d",
                        symfonyApiUrl, limit, offset);

                if (symfonyOrderId != null) {
                    // Fetch single order
                    url = String.format("%s/gpt/orders/%s", symfonyApiUrl, symfonyOrderId);
                    hasMore = false;
                }

                log.info(" Fetching orders with cards from: {}", url);

                Map<String, Object> response = getFromSymfonyApi(url);

                if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                    throw new RuntimeException("Invalid response from Symfony API");
                }

                List<Map<String, Object>> ordersData;
                if (symfonyOrderId != null) {
                    // Single order response
                    Map<String, Object> orderData = (Map<String, Object>) response.get("data");
                    ordersData = Collections.singletonList(orderData);
                } else {
                    ordersData = (List<Map<String, Object>>) response.get("data");
                }

                if (ordersData == null || ordersData.isEmpty()) {
                    hasMore = false;
                    continue;
                }

                // Process each order's card certifications
                for (Map<String, Object> orderData : ordersData) {
                    List<Map<String, Object>> cardCerts =
                            (List<Map<String, Object>>) orderData.get("card_certifications");

                    if (cardCerts != null && !cardCerts.isEmpty()) {
                        String orderSymfonyId = getString(orderData, "id");
                        int syncedCards = cardSyncService.syncCardsFromGptApi(orderSymfonyId, cardCerts);
                        totalCardsSynced += syncedCards;

                        log.debug(" Synced {} cards for order {}", syncedCards, orderSymfonyId);
                    }
                }

                offset += limit;
                if (symfonyOrderId == null) {
                    Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                    int total = meta != null ? getInteger(meta, "total", 0) : 0;
                    hasMore = ordersData.size() == limit && offset < total;
                }

                progressPublisher.publishProgress(syncId,
                        SyncProgress.processing(syncId, "CARDS",
                                String.format("Synced %d cards...", totalCardsSynced),
                                totalCardsSynced, totalCardsSynced));
            }

            long duration = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("synced", totalCardsSynced);
            result.put("duration_ms", duration);
            result.put("message", String.format("Synced %d cards in %.1f seconds",
                    totalCardsSynced, duration / 1000.0));
            result.put("syncId", syncId);

            progressPublisher.publishProgress(syncId,
                    SyncProgress.completed(syncId, "CARDS",
                            String.format("Cards sync completed: %d cards", totalCardsSynced),
                            totalCardsSynced));

            log.info(" Cards sync completed: {} cards in {}ms", totalCardsSynced, duration);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error(" Error during cards sync", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("syncId", syncId);

            progressPublisher.publishError(syncId, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Get sync status and health check
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Local counts
            long localOrders = orderRepository.count();
            status.put("local_orders", localOrders);

            // Symfony counts via new API
            Integer symfonyOrders = getSymfonyOrderCount();
            status.put("symfony_orders", symfonyOrders);

            // Sync status
            boolean inSync = symfonyOrders != null && symfonyOrders == localOrders;
            status.put("in_sync", inSync);
            status.put("difference", symfonyOrders != null ? symfonyOrders - localOrders : null);

            status.put("success", true);
            status.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            log.error(" Error getting sync status", e);
            status.put("success", false);
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }

    /**
     * Health check for Symfony API connection
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Test connection to new GptOrderController
            String url = symfonyApiUrl + "/gpt/orders?limit=1";
            Map<String, Object> response = getFromSymfonyApi(url);

            boolean symfonyHealthy = response != null && Boolean.TRUE.equals(response.get("success"));

            health.put("status", symfonyHealthy ? "healthy" : "unhealthy");
            health.put("symfony_api", symfonyHealthy ? "connected" : "disconnected");
            health.put("symfony_url", symfonyApiUrl);
            health.put("api_endpoint", "/gpt/orders");
            health.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("symfony_api", "error");
            health.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get Symfony order count via new API
     */
    private Integer getSymfonyOrderCount() {
        try {
            String url = symfonyApiUrl + "/gpt/orders?limit=1&offset=0";
            Map<String, Object> response = getFromSymfonyApi(url);

            if (response != null && response.containsKey("meta")) {
                Map<String, Object> meta = (Map<String, Object>) response.get("meta");
                return getInteger(meta, "total", 0);
            }

            return 0;
        } catch (Exception e) {
            log.error(" Error getting Symfony order count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Convert Symfony status code to OrderStatus enum
     *
     * Symfony status codes (from OrderStatus enum):
     * 1 = STATUS_A_RECEP (To be received) -> PENDING
     * 9 = STATUS_PACKAGE_ACCEPTED (Package accepted) -> PENDING
     * 10 = STATUS_A_SCANNER (To be scanned) -> SCANNING
     * 11 = STATUS_A_OUVRIR (To be opened) -> PENDING
     * 2 = STATUS_A_NOTER (To be graded) -> GRADING
     * 12 = STATUS_A_NOTER2 (To be graded 2) -> GRADING
     * 13 = STATUS_A_NOTER3 (To be graded 3) -> GRADING
     * 3 = STATUS_A_CERTIFIER (To be certified) -> CERTIFYING
     * 4 = STATUS_A_PREPARER (To be prepared) -> PACKAGING
     * 42 = STATUS_A_ENVOYER (To be sent) -> SHIPPING
     * 41 = STATUS_A_DISTRIBUER (To be distributed) -> SHIPPING
     * 5 = STATUS_ENVOYEE (Sent) -> DELIVERED
     */
    private OrderStatus convertSymfonyStatusToOrderStatus(Integer statusCode) {
        if (statusCode == null) {
            return OrderStatus.PENDING;
        }

        switch (statusCode) {
            case 1:  // STATUS_A_RECEP
            case 9:  // STATUS_PACKAGE_ACCEPTED
            case 11: // STATUS_A_OUVRIR
                return OrderStatus.PENDING;
            case 2:  // STATUS_A_NOTER
            case 12: // STATUS_A_NOTER2
            case 13: // STATUS_A_NOTER3
                return OrderStatus.GRADING;
            case 3:  // STATUS_A_CERTIFIER
                return OrderStatus.CERTIFYING;
            case 4:  // STATUS_A_PREPARER
                return OrderStatus.PACKAGING;
            case 10: // STATUS_A_SCANNER
                return OrderStatus.SCANNING;
            case 41: // STATUS_A_DISTRIBUER
            case 42: // STATUS_A_ENVOYER
                return OrderStatus.PACKAGING;
            case 5:  // STATUS_ENVOYEE
                return OrderStatus.DELIVERED;
            default:
                log.debug("Unknown Symfony status code: {}, defaulting to PENDING", statusCode);
                return OrderStatus.PENDING;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        String value = getString(map, key);
        return value != null ? value : defaultValue;
    }

    private Integer getInteger(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Float getFloat(Map<String, Object> map, String key, float defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parse datetime string from Symfony API
     * Supports formats: "yyyy-MM-dd HH:mm:ss" and "yyyy-MM-dd"
     */
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            if (dateStr.contains(" ")) {
                return LocalDateTime.parse(dateStr, DATE_FORMATTER);
            } else {
                return LocalDateTime.parse(dateStr + " 00:00:00", DATE_FORMATTER);
            }
        } catch (Exception e) {
            log.warn(" Could not parse date: {}", dateStr);
            return LocalDateTime.now();
        }
    }

    /**
     * Create HTTP headers with Symfony API authentication
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (symfonyApiKey != null && !symfonyApiKey.isEmpty()) {
            headers.set("Authorization", "Bearer " + symfonyApiKey);
        }
        return headers;
    }

    /**
     * Make authenticated GET request to Symfony API
     */
    private Map<String, Object> getFromSymfonyApi(String url) {
        HttpHeaders headers = createAuthHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return response.getBody();
    }
}
