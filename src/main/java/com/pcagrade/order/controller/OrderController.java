package com.pcagrade.order.controller;

import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import com.pcagrade.order.PlanningApplication;
import com.pcagrade.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

import java.time.LocalDate;



import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import com.pcagrade.order.entity.Order;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    /**
     * ✅ FIXED - Main endpoint now uses correct priority mapping
     * GET /api/orders - Main endpoint for orders (expected by frontend)
     */
    @GetMapping("")
    @Operation(summary = "Get recent orders with priority mapping", description = "Retrieve orders with correct delai->priority mapping")
    public ResponseEntity<List<Map<String, Object>>> getOrders() {
        try {
            log.info("📦 GET /api/orders called - Using CORRECTED priority mapping");

            // ✅ CALL THE NEW METHOD WITH PRIORITY MAPPING
            List<Map<String, Object>> orders = orderService.getRecentOrdersWithPriorityMapping();

            // ✅ LOG SAMPLE TO VERIFY PRIORITIES
            if (!orders.isEmpty()) {
                Map<String, Object> firstOrder = orders.get(0);
                log.info("📋 Sample order: {} - delai: '{}' -> priority: '{}'",
                        firstOrder.get("orderNumber"),
                        firstOrder.get("delai"),
                        firstOrder.get("priority"));
            }

            // ✅ LOG PRIORITY DISTRIBUTION FOR FRONTEND
            Map<String, Long> priorityCount = orders.stream()
                    .collect(Collectors.groupingBy(
                            o -> (String) o.get("priority"),
                            Collectors.counting()
                    ));
            log.info("📊 FRONTEND will receive priority distribution: {}", priorityCount);

            log.info("✅ Returning {} orders with correct priorities", orders.size());
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            log.error("❌ Error in GET /api/orders", e);
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * 🧪 TEST ENDPOINT - Quick test to verify priority mapping
     */
    @GetMapping("/test-priority-mapping-simple")
    public ResponseEntity<Map<String, Object>> testPriorityMappingSimple() {
        try {
            log.info("🧪 Testing simple priority mapping");

            String sql = """
        SELECT 
            o.delai,
            COUNT(*) as count,
            CASE 
                WHEN o.delai = 'X' THEN 'EXCELSIOR'
                WHEN o.delai = 'F+' THEN 'FAST_PLUS'  
                WHEN o.delai = 'F' THEN 'FAST'
                WHEN o.delai IN ('C', 'E') THEN 'CLASSIC'
                ELSE 'UNKNOWN'
            END as mapped_priority
        FROM `order` o 
        WHERE o.annulee = 0
        GROUP BY o.delai
        ORDER BY COUNT(*) DESC
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            Map<String, Object> response = new HashMap<>();
            List<Map<String, Object>> mappings = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> mapping = new HashMap<>();
                mapping.put("delai", row[0]);
                mapping.put("count", ((Number) row[1]).intValue());
                mapping.put("mappedPriority", row[2]);
                mappings.add(mapping);
            }

            response.put("success", true);
            response.put("mappings", mappings);
            response.put("message", "Priority mapping test completed");

            log.info("🧪 Priority mapping test results: {}", mappings);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Priority mapping test failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/frontend/orders")
    public ResponseEntity<List<Map<String, Object>>> getOrdersFrontend() {
        try {
            System.out.println("Frontend: Retrieving orders with real data");

            // Use the correct method that returns List<Map<String, Object>>
            List<Map<String, Object>> orders = orderService.getRecentOrdersAsMap();

            System.out.println("" + orders.size() + " orders returned");
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            System.err.println("Error retrieving orders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * GET /api/orders/{id}/cards - Get cards for a specific order
     * Fixed version with proper error handling and data mapping
     */
    @GetMapping("/{id}/cards")
    public ResponseEntity<Map<String, Object>> getOrderCards(@PathVariable String id) {
        try {
            System.out.println("🃏 Loading cards for order: " + id);

            // Query to get individual cards for an order
            String sql = """
            SELECT 
                HEX(cc.id) as cardId,
                COALESCE(cc.code_barre, cc.barcode, 'N/A') as barcode,
                COALESCE(cc.type, 'Pokemon') as cardType,
                COALESCE(cc.annotation, '') as annotation,
                COALESCE(ct.name, CONCAT('Pokemon Card #', COALESCE(cc.code_barre, cc.id))) as cardName,
                COALESCE(ct.label_name, ct.name) as labelName,
                3 as processingTime
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
            WHERE HEX(cco.order_id) = ?
            ORDER BY COALESCE(cc.code_barre, cc.id) ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, id.toUpperCase().replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();
            int cardsWithName = 0;

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();

                card.put("cardId", row[0]);
                card.put("barcode", row[1]);
                card.put("type", row[2]);
                card.put("annotation", row[3]);
                card.put("name", row[4]);
                card.put("labelName", row[5]);
                card.put("processingTime", row[6]);

                // Count cards with names (not null and not empty)
                String cardName = (String) row[4];
                if (cardName != null && !cardName.trim().isEmpty() &&
                        !cardName.startsWith("Pokemon Card #")) {
                    cardsWithName++;
                }

                cards.add(card);
            }

            // Calculate statistics
            int totalCards = cards.size();
            double namePercentage = totalCards > 0
                    ? Math.round((cardsWithName / (double) totalCards) * 100)
                    : 0;

            // Response object
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", id);
            response.put("cards", cards);
            response.put("totalCards", totalCards);
            response.put("cardsWithName", cardsWithName);
            response.put("namePercentage", namePercentage);
            response.put("estimatedDuration", totalCards * 3);

            System.out.println("✅ Found " + totalCards + " cards for order " + id +
                    " (" + cardsWithName + " with names = " + namePercentage + "%)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error loading cards for order " + id + ": " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orderId", id);
            errorResponse.put("cards", new ArrayList<>());
            errorResponse.put("totalCards", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Get all orders with pagination support
     */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            System.out.println("Retrieving all orders - page: " + page + ", size: " + size);

            List<Map<String, Object>> orders = orderService.getAllOrdersAsMap();

            // Simple pagination
            int start = page * size;
            int end = Math.min(start + size, orders.size());

            if (start >= orders.size()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> paginatedOrders = orders.subList(start, end);

            System.out.println("Returning " + paginatedOrders.size() + " orders (page " + page + ")");
            return ResponseEntity.ok(paginatedOrders);

        } catch (Exception e) {
            System.err.println("Error retrieving all orders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Get orders for planning since a specific date
     */
    @GetMapping("/planning")
    public ResponseEntity<List<Map<String, Object>>> getOrdersForPlanning(
            @RequestParam int day,
            @RequestParam int month,
            @RequestParam int year) {
        try {
            System.out.println("Retrieving orders for planning since: " + day + "/" + month + "/" + year);

            List<Map<String, Object>> orders = orderService.getOrdersForPlanning(day, month, year);

            System.out.println("" + orders.size() + " orders found for planning");
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            System.err.println("Error retrieving orders for planning: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Get order statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getOrderStatistics() {
        try {
            System.out.println("Retrieving order statistics");

            Map<String, Object> statistics = orderService.getOrderStatistics();

            System.out.println("Order statistics retrieved successfully");
            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            System.err.println("Error retrieving order statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Search orders by various criteria
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchOrders(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        try {
            System.out.println("Searching orders with criteria: " + searchTerm + ", " + status + ", " + priority);

            // Convert string parameters to enums if provided
            Integer orderStatusCode = null;
            if (status != null && !status.isEmpty()) {
                try {
                    // Try parsing as integer directly (since statuses are stored as integers)
                    orderStatusCode = Integer.parseInt(status);
                } catch (NumberFormatException e) {
                    // If not a number, try mapping from string names to status codes
                    switch (status.toUpperCase()) {
                        case "PENDING", "TO_RECEIVE" -> orderStatusCode = Order.STATUS_A_RECEPTIONNER;
                        case "PACKAGE_ACCEPTED" -> orderStatusCode = Order.STATUS_COLIS_ACCEPTE;
                        case "TO_SCAN" -> orderStatusCode = Order.STATUS_A_SCANNER;
                        case "TO_OPEN" -> orderStatusCode = Order.STATUS_A_OUVRIR;
                        case "TO_EVALUATE" -> orderStatusCode = Order.STATUS_A_NOTER;
                        case "TO_CERTIFY" -> orderStatusCode = Order.STATUS_A_CERTIFIER;
                        case "TO_PREPARE" -> orderStatusCode = Order.STATUS_A_PREPARER;
                        case "TO_UNSEAL" -> orderStatusCode = Order.STATUS_A_DESCELLER;
                        case "TO_SEE" -> orderStatusCode = Order.STATUS_A_VOIR;
                        case "TO_DISTRIBUTE" -> orderStatusCode = Order.STATUS_A_DISTRIBUER;
                        case "TO_SEND" -> orderStatusCode = Order.STATUS_A_ENVOYER;
                        case "SENT" -> orderStatusCode = Order.STATUS_ENVOYEE;
                        case "RECEIVED" -> orderStatusCode = Order.STATUS_RECU;
                        default -> {
                            System.err.println("Unknown status string: " + status);
                            orderStatusCode = null;
                        }
                    }
                }
            }

            Order.OrderPriority orderPriority = null;
            if (priority != null && !priority.isEmpty()) {
                try {
                    orderPriority = Order.OrderPriority.valueOf(priority.toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid priority: " + priority);
                }
            }

// Update the service call to use Integer instead of OrderStatus
            List<Order> orders = orderService.searchOrders(searchTerm, orderStatusCode, orderPriority);

            List<Map<String, Object>> orderMaps = orders.stream()
                    .map(order -> {
                        Map<String, Object> orderMap = Map.of(
                                "id", order.getId().toString(),
                                "orderNumber", order.getOrderNumber(),
                                "customerName", order.getCustomerName(),
                                "orderDate", order.getOrderDate(),
                                "status", order.getStatusText(),
                                "priority", order.getPriority().name(),
                                "cardCount", order.getCardCount(),
                                "totalPrice", order.getTotalPrice()
                        );
                        return orderMap;
                    })
                    .toList();

            System.out.println("" + orderMaps.size() + " orders found matching criteria");
            return ResponseEntity.ok(orderMaps);

        } catch (Exception e) {
            System.err.println("Error searching orders: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ===============================================
    // ADD THIS METHOD TO YOUR EXISTING CONTROLLER
    // ===============================================

    /**
     * ORDERS SINCE JUNE 1, 2025 ENDPOINT (ENGLISH)
     *
     * Returns real orders from database where date >= '2025-06-01'
     * This uses the 'date' field which is the order creation date
     */
    @GetMapping("/api/orders/since-june-2025")
    public ResponseEntity<List<Map<String, Object>>> getOrdersSinceJune2025() {
        try {
            System.out.println("=== ORDERS SINCE JUNE 1, 2025 ===");

            // SQL query to get orders since June 1, 2025
            String sqlOrders = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                COALESCE(o.priority_string, 'FAST') as priority,
                o.status,
                DATE(o.date) as creationDate,
                o.date as fullTimestamp,
                COALESCE(o.temps_estime_minutes, 0) as estimatedTimeMinutes,
                COALESCE(o.prix_total, 0) as totalPrice,
                (SELECT COUNT(*) FROM j_certification c WHERE c.order_id = o.id) as cardCount,
                (SELECT COUNT(*) FROM j_certification c WHERE c.order_id = o.id AND c.nom IS NOT NULL AND c.nom != '') as cardsWithName
            FROM `order` o 
            WHERE o.date >= '2025-06-01'
            ORDER BY o.date DESC
        """;

            Query query = entityManager.createNativeQuery(sqlOrders);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();

                // Basic order info
                order.put("id", row[0]);
                order.put("orderNumber", row[1]);
                order.put("priority", row[2]);
                order.put("status", row[3]);
                order.put("creationDate", row[4]);
                order.put("fullTimestamp", row[5]);
                order.put("estimatedTimeMinutes", row[6]);
                order.put("totalPrice", row[7]);

                // Card statistics
                Number cardCount = (Number) row[8];
                Number cardsWithName = (Number) row[9];

                order.put("cardCount", cardCount.intValue());
                order.put("cardsWithName", cardsWithName.intValue());

                // Calculate percentage
                double namePercentage = cardCount.intValue() > 0 ?
                        (cardsWithName.doubleValue() / cardCount.doubleValue()) * 100 : 0;
                order.put("namePercentage", Math.round(namePercentage));

                // Quality indicator
                if (namePercentage >= 95) {
                    order.put("qualityIndicator", "");
                } else if (namePercentage >= 80) {
                    order.put("qualityIndicator", "");
                } else {
                    order.put("qualityIndicator", "");
                }

                // Additional fields for frontend compatibility
                order.put("estimatedTimeHours", String.format("%.1fh", ((Number) row[6]).doubleValue() / 60));
                order.put("orderDate", row[4]); // Same as creationDate
                order.put("deadline", calculateDeadline((String) row[4])); // Creation date + 7 days

                orders.add(order);
            }

            System.out.println("Found " + orders.size() + " orders since June 1, 2025");

            // Log sample for debugging
            if (!orders.isEmpty()) {
                System.out.println("Sample order: " + orders.get(0));
            }

            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            System.err.println("Error getting orders since June 2025: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * ORDERS WITH DATE FILTER ENDPOINT (ENGLISH)
     *
     * More flexible endpoint that accepts a date parameter
     */
    @GetMapping("/api/orders/since")
    public ResponseEntity<List<Map<String, Object>>> getOrdersSince(
            @RequestParam(defaultValue = "2025-06-01") String sinceDate) {
        try {
            System.out.println("=== ORDERS SINCE " + sinceDate + " ===");

            String sqlOrders = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                COALESCE(o.priority_string, 'FAST') as priority,
                o.status,
                DATE(o.date) as creationDate,
                o.date as fullTimestamp,
                COALESCE(o.temps_estime_minutes, 0) as estimatedTimeMinutes,
                COALESCE(o.prix_total, 0) as totalPrice,
                (SELECT COUNT(*) FROM j_certification c WHERE c.order_id = o.id) as cardCount,
                (SELECT COUNT(*) FROM j_certification c WHERE c.order_id = o.id AND c.nom IS NOT NULL AND c.nom != '') as cardsWithName
            FROM `order` o 
            WHERE o.date >= ?
            ORDER BY o.date DESC
        """;

            Query query = entityManager.createNativeQuery(sqlOrders);
            query.setParameter(1, sinceDate);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();

                order.put("id", row[0]);
                order.put("orderNumber", row[1]);
                order.put("priority", row[2]);
                order.put("status", mapStatusToText((Number) row[3]));
                order.put("creationDate", row[4]);
                order.put("fullTimestamp", row[5]);
                order.put("estimatedTimeMinutes", row[6]);
                order.put("totalPrice", row[7]);

                Number cardCount = (Number) row[8];
                Number cardsWithName = (Number) row[9];

                order.put("cardCount", cardCount.intValue());
                order.put("cardsWithName", cardsWithName.intValue());

                double namePercentage = cardCount.intValue() > 0 ?
                        (cardsWithName.doubleValue() / cardCount.doubleValue()) * 100 : 0;
                order.put("namePercentage", Math.round(namePercentage));

                if (namePercentage >= 95) {
                    order.put("qualityIndicator", "");
                } else if (namePercentage >= 80) {
                    order.put("qualityIndicator", "");
                } else {
                    order.put("qualityIndicator", "");
                }

                order.put("estimatedTimeHours", String.format("%.1fh", ((Number) row[6]).doubleValue() / 60));
                order.put("orderDate", row[4]);
                order.put("deadline", calculateDeadline((String) row[4]));

                orders.add(order);
            }

            System.out.println("Found " + orders.size() + " orders since " + sinceDate);
            return ResponseEntity.ok(orders);

        } catch (Exception e) {
            System.err.println("Error getting orders since " + sinceDate + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ===============================================
    // UTILITY METHODS
    // ===============================================



    /**
     * Calculate deadline (creation date + 7 days)
     */
    private String calculateDeadline(String creationDate) {
        try {
            if (creationDate == null) return null;

            // Parse the date and add 7 days
            LocalDate date = LocalDate.parse(creationDate);
            LocalDate deadline = date.plusDays(7);
            return deadline.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ========== Alternative endpoint pour debug ==========
    /**
     * GET /api/orders/{id}/cards/debug - Debug version with more detailed logging
     */
    @GetMapping("/{id}/cards/debug")
    public ResponseEntity<Map<String, Object>> getOrderCardsDebug(@PathVariable String id) {
        try {
            System.out.println("🔍 DEBUG: Loading cards for order: " + id);

            // First, check if the order exists
            String checkOrderSql = "SELECT HEX(id), num_commande FROM `order` WHERE HEX(id) = ?";
            Query checkQuery = entityManager.createNativeQuery(checkOrderSql);
            checkQuery.setParameter(1, id.toUpperCase().replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> orderCheck = checkQuery.getResultList();

            if (orderCheck.isEmpty()) {
                System.out.println("❌ Order not found: " + id);
                return ResponseEntity.notFound().build();
            }

            System.out.println("✅ Order found: " + orderCheck.get(0)[1]);

            // Check if there are any card associations
            String countSql = "SELECT COUNT(*) FROM card_certification_order WHERE HEX(order_id) = ?";
            Query countQuery = entityManager.createNativeQuery(countSql);
            countQuery.setParameter(1, id.toUpperCase().replace("-", ""));
            Number cardCount = (Number) countQuery.getSingleResult();

            System.out.println("📊 Found " + cardCount + " card associations");

            // If no cards, return empty but valid response
            if (cardCount.intValue() == 0) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("orderId", id);
                response.put("cards", new ArrayList<>());
                response.put("totalCards", 0);
                response.put("cardsWithName", 0);
                response.put("namePercentage", 0);
                response.put("message", "Order exists but has no cards associated");
                return ResponseEntity.ok(response);
            }

            // Get detailed card information
            String sql = """
            SELECT 
                HEX(cc.id) as cardId,
                COALESCE(cc.code_barre, 'NO-BARCODE') as barcode,
                COALESCE(cc.type, 'Pokemon') as cardType,
                COALESCE(cc.annotation, '') as annotation,
                COALESCE(ct.name, CONCAT('Card-', HEX(cc.id))) as cardName,
                CASE 
                    WHEN ct.name IS NOT NULL AND ct.name != '' THEN 1 
                    ELSE 0 
                END as hasName
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
            WHERE HEX(cco.order_id) = ?
            ORDER BY cc.code_barre ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, id.toUpperCase().replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cards = new ArrayList<>();
            int cardsWithName = 0;

            for (Object[] row : results) {
                Map<String, Object> card = new HashMap<>();
                card.put("cardId", row[0]);
                card.put("barcode", row[1]);
                card.put("type", row[2]);
                card.put("annotation", row[3]);
                card.put("name", row[4]);

                Number hasName = (Number) row[5];
                if (hasName.intValue() == 1) {
                    cardsWithName++;
                }

                cards.add(card);
            }

            double namePercentage = cards.size() > 0
                    ? Math.round((cardsWithName / (double) cards.size()) * 100)
                    : 0;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", id);
            response.put("cards", cards);
            response.put("totalCards", cards.size());
            response.put("cardsWithName", cardsWithName);
            response.put("namePercentage", namePercentage);

            System.out.println("✅ DEBUG: Successfully loaded " + cards.size() + " cards");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ DEBUG ERROR: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("debug", true);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    private String mapStatusToText(Number statusNumber) {
        if (statusNumber == null) return "Unknown";

        int status = statusNumber.intValue();
        return switch (status) {
            case Order.STATUS_A_RECEPTIONNER -> "A_RECEPTIONNER";
            case Order.STATUS_COLIS_ACCEPTE -> "COLIS_ACCEPTE";
            case Order.STATUS_A_SCANNER -> "A_SCANNER";
            case Order.STATUS_A_OUVRIR -> "A_OUVRIR";
            case Order.STATUS_A_NOTER -> "A_NOTER";
            case Order.STATUS_A_CERTIFIER -> "A_CERTIFIER";
            case Order.STATUS_A_PREPARER -> "A_PREPARER";
            case Order.STATUS_A_DESCELLER -> "A_DESCELLER";
            case Order.STATUS_A_VOIR -> "A_VOIR";
            case Order.STATUS_A_DISTRIBUER -> "A_DISTRIBUER";
            case Order.STATUS_A_ENVOYER -> "A_ENVOYER";
            case Order.STATUS_ENVOYEE -> "ENVOYEE";
            case Order.STATUS_RECU -> "RECU";
            default -> "UNKNOWN";
        };

    }

    /**
     * 🔍 DEBUG ENDPOINT - Get priority mapping details
     * Endpoint: GET /api/orders/debug/priorities
     */
    @GetMapping("/debug/priorities")
    public ResponseEntity<Map<String, Object>> debugPriorities() {
        try {
            log.info("🔍 Debug: Analyzing priority mappings");

            String sql = """
        SELECT 
            o.delai as original_delai,
            COUNT(*) as count,
            GROUP_CONCAT(DISTINCT o.num_commande ORDER BY o.num_commande LIMIT 5) as sample_orders
        FROM `order` o
        WHERE o.annulee = 0
        GROUP BY o.delai
        ORDER BY COUNT(*) DESC
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> delaiAnalysis = new ArrayList<>();

            for (Object[] row : results) {
                String delai = (String) row[0];
                Integer count = ((Number) row[1]).intValue();
                String samples = (String) row[2];

                Map<String, Object> analysis = new HashMap<>();
                analysis.put("originalDelai", delai);
                analysis.put("mappedPriority", Order.mapDelaiToPriority(delai).name());
                analysis.put("count", count);
                analysis.put("sampleOrders", samples != null ? samples.split(",") : new String[0]);
                analysis.put("isValid", Set.of("X", "F+", "F", "C", "E").contains(delai));

                delaiAnalysis.add(analysis);
            }

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("delaiAnalysis", delaiAnalysis);
            debugInfo.put("validDelaiValues", List.of("X", "F+", "F", "C", "E"));
            debugInfo.put("priorityMapping", Map.of(
                    "X", "EXCELSIOR",
                    "F+", "FAST_PLUS",
                    "F", "FAST",
                    "C", "CLASSIC",
                    "E", "CLASSIC"
            ));
            debugInfo.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("❌ Error in priority debug", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * 🔍 DEBUG ENDPOINT - Check actual delai values in database
     * Endpoint: GET /api/orders/debug/delai-distribution
     */
    @GetMapping("/debug/delai-distribution")
    public ResponseEntity<Map<String, Object>> debugDelaiDistribution() {
        try {
            log.info("🔍 Debug: Analyzing delai column distribution");

            String sql = """
        SELECT 
            COALESCE(o.delai, 'NULL') as delai_value,
            COUNT(*) as count,
            ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM `order` WHERE annulee = 0), 2) as percentage,
            GROUP_CONCAT(DISTINCT o.num_commande ORDER BY o.num_commande LIMIT 5) as sample_orders,
            CASE
               WHEN o.delai = 'X' THEN 'EXCELSIOR'
               WHEN o.delai = 'F+' THEN 'FAST_PLUS'
               WHEN o.delai = 'F' THEN 'FAST'
               WHEN o.delai IN ('C', 'E') THEN 'CLASSIC'
               ELSE 'UNMAPPED'
            END as mapped_priority
        FROM `order` o
        WHERE o.annulee = 0
        GROUP BY o.delai
        ORDER BY COUNT(*) DESC
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> distribution = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("delaiValue", (String) row[0]);
                item.put("count", ((Number) row[1]).intValue());
                item.put("percentage", ((Number) row[2]).doubleValue());
                item.put("sampleOrders", row[3] != null ? Arrays.asList(((String) row[3]).split(",")) : new ArrayList<>());
                item.put("mappedPriority", (String) row[4]);

                distribution.add(item);
            }

            // Get total count
            String totalSql = "SELECT COUNT(*) FROM `order` WHERE annulee = 0";
            Query totalQuery = entityManager.createNativeQuery(totalSql);
            Number totalCount = (Number) totalQuery.getSingleResult();

            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalOrders", totalCount.intValue());
            debugInfo.put("delaiDistribution", distribution);
            debugInfo.put("mapping", Map.of(
                    "X", "EXCELSIOR",
                    "F+", "FAST_PLUS",
                    "F", "FAST",
                    "C", "CLASSIC",
                    "E", "CLASSIC"
            ));
            debugInfo.put("timestamp", LocalDateTime.now());

            log.info("📊 Delai distribution: {} unique values found", distribution.size());
            return ResponseEntity.ok(debugInfo);

        } catch (Exception e) {
            log.error("❌ Error in delai distribution debug", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * 🧪 TEST ENDPOINT - Verify priority mapping is working
     * Endpoint: GET /api/orders/test/priority-mapping
     */
    @GetMapping("/test/priority-mapping")
    public ResponseEntity<Map<String, Object>> testPriorityMapping() {
        try {
            log.info("🧪 Testing priority mapping from delai column");

            // Get a sample of orders with their delai and mapped priorities
            String sql = """
        SELECT 
            HEX(o.id) as id,
            o.num_commande as orderNumber,
            COALESCE(o.delai, 'NULL') as delai,
            CASE
               WHEN o.delai = 'X' THEN 'EXCELSIOR'
               WHEN o.delai = 'F+' THEN 'FAST_PLUS'
               WHEN o.delai = 'F' THEN 'FAST'
               WHEN o.delai IN ('C', 'E') THEN 'CLASSIC'
               ELSE 'UNMAPPED'
            END as mappedPriority
        FROM `order` o
        WHERE o.annulee = 0
        ORDER BY o.date DESC
        LIMIT 20
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> samples = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> sample = new HashMap<>();
                sample.put("id", (String) row[0]);
                sample.put("orderNumber", (String) row[1]);
                sample.put("delai", (String) row[2]);
                sample.put("mappedPriority", (String) row[3]);
                samples.add(sample);
            }

            // Get statistics
            String statsSql = """
        SELECT 
            COALESCE(o.delai, 'NULL') as delai,
            COUNT(*) as count
        FROM `order` o
        WHERE o.annulee = 0
        GROUP BY o.delai
        ORDER BY COUNT(*) DESC
        """;

            Query statsQuery = entityManager.createNativeQuery(statsSql);
            @SuppressWarnings("unchecked")
            List<Object[]> statsResults = statsQuery.getResultList();

            Map<String, Integer> delaiStats = new HashMap<>();
            for (Object[] row : statsResults) {
                delaiStats.put((String) row[0], ((Number) row[1]).intValue());
            }

            Map<String, Object> testResult = new HashMap<>();
            testResult.put("success", true);
            testResult.put("sampleOrders", samples);
            testResult.put("delaiStatistics", delaiStats);
            testResult.put("mapping", Map.of(
                    "X", "EXCELSIOR",
                    "F+", "FAST_PLUS",
                    "F", "FAST",
                    "C", "CLASSIC",
                    "E", "CLASSIC"
            ));
            testResult.put("message", "Priority mapping test completed");
            testResult.put("timestamp", LocalDateTime.now());

            log.info("✅ Priority mapping test completed. Found {} unique delai values", delaiStats.size());
            return ResponseEntity.ok(testResult);

        } catch (Exception e) {
            log.error("❌ Error in priority mapping test", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "success", false,
                            "error", e.getMessage(),
                            "message", "Priority mapping test failed"
                    ));
        }
    }

}