package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

//========== MISSING IMPORTS TO FIX ERRORS ==========
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Validated
@Slf4j
public class OrderService {
    private static final Integer DEFAULT_STATUS = Order.STATUS_A_RECEPTIONNER;
    private static final int DEFAULT_PROCESSING_TIME_PER_CARD = 3; // minutes per card
    private static final int MAX_CARDS_PER_ORDER = 1000;
    private static final int MIN_CARDS_PER_ORDER = 1;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new order
     * @param order the order to create
     * @return created order
     */
    public Order createOrder(@Valid @NotNull Order order) {
        log.info("Creating new order: {}", order.getOrderNumber());

        // Validate business rules
        validateNewOrder(order);

        // Set default values
        if (order.getStatus() == null) {
            order.setStatus(Order.OrderStatus.PENDING);
        }
        if (order.getEstimatedTimeMinutes() == null && order.getCardCount() != null) {
            order.setEstimatedTimeMinutes(calculateEstimatedTime(order.getCardCount()));
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDate.now());
        }
        if (order.getPriority() == null) {
            order.setPriority(Order.OrderPriority.FAST);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        return savedOrder;
    }

    /**
     * Update an existing order
     * @param order the order to update
     * @return updated order
     */
    public Order updateOrder(@Valid @NotNull Order order) {
        log.info("Updating order: {}", order.getId());

        if (!orderRepository.existsById(order.getId())) {
            throw new IllegalArgumentException("Order not found with ID: " + order.getId());
        }

        // Recalculate estimated time if card count changed
        if (order.getCardCount() != null && order.getEstimatedTimeMinutes() == null) {
            order.setEstimatedTimeMinutes(calculateEstimatedTime(order.getCardCount()));
        }

        Order updatedOrder = orderRepository.save(order);
        log.info("Order updated successfully: {}", updatedOrder.getId());
        return updatedOrder;
    }

    /**
     * Find order by ID
     * @param id the order ID
     * @return optional order
     */
    @Transactional(readOnly = true)
    public Optional<Order> findById(@NotNull UUID id) {
        return orderRepository.findById(id);
    }

    /**
     * Find order by order number
     * @param orderNumber the order number
     * @return optional order
     */
    @Transactional(readOnly = true)
    public Optional<Order> findByOrderNumber(@NotNull String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    /**
     * Get all orders with pagination
     * @param pageable pagination information
     * @return page of orders
     */
    @Transactional(readOnly = true)
    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    /**
     * Delete an order
     * @param id the order ID
     */
    public void deleteOrder(@NotNull UUID id) {
        log.info("Deleting order: {}", id);

        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order not found with ID: " + id);
        }

        orderRepository.deleteById(id);
        log.info("Order deleted successfully: {}", id);
    }

    // ========== ORDER STATUS OPERATIONS ==========



    private Map<String, Object> convertOrderToMap(Order order) {
        Map<String, Object> orderMap = new HashMap<>();
        if (order != null) {
            orderMap.put("id", order.getId() != null ? order.getId().toString() : null);
            orderMap.put("orderNumber", order.getOrderNumber());
            orderMap.put("cardCount", order.getCardCount());
            orderMap.put("estimatedTimeMinutes", order.getEstimatedTimeMinutes());
            orderMap.put("priority", order.getPriority() != null ? order.getPriority().name() : null);
            orderMap.put("status", order.getStatus() != null ? order.getStatusText() : null);
            orderMap.put("orderDate", order.getOrderDate());
            orderMap.put("totalPrice", order.getTotalPrice());
        }
        return orderMap;
    }



    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Calculate estimated processing time
     * @param cardCount number of cards
     * @return estimated time in minutes
     */
    public int calculateEstimatedTime(@Positive int cardCount) {
        return cardCount * DEFAULT_PROCESSING_TIME_PER_CARD;
    }

    /**
     * Get orders that need planning from a specific date - FIXED VERSION WITHOUT DUPLICATES
     * Excludes orders that are already planned in j_planning table
     * @param day day of the month
     * @param month month (1-12)
     * @param year year
     * @return list of orders as maps for compatibility (excluding already planned)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOrdersForPlanning(int day, int month, int year) {
        try {
            log.info(" Loading orders for planning since {}/{}/{} (excluding already planned)", day, month, year);

            String fromDate = String.format("%04d-%02d-%02d", year, month, day);

            String sql = """
            SELECT DISTINCT
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                o.date as orderDate,
                COALESCE(o.delai, 'FAST') as deadline,
                (SELECT COUNT(*) FROM card_certification_order cco 
                     WHERE cco.order_id = o.id) as cardCount,
                CASE
                   WHEN o.delai = 'X' THEN 'Excelsior'
                   WHEN o.delai = 'F+' THEN 'Fast+'
                   WHEN o.delai = 'F' THEN 'Fast'
                   WHEN o.delai = 'C' THEN 'Classic'
                   ELSE 'Classic'
                END as priority,
                o.status,
                o.prix_total as totalPrice
            FROM `order` o
            WHERE o.date >= ?
            AND o.status IN (1, 2)
            AND COALESCE(o.annulee, 0) = 0
            AND NOT EXISTS (
                SELECT 1 FROM j_planning jp 
                WHERE jp.order_id = o.id
            )
            ORDER BY o.date ASC
            LIMIT 1000
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, fromDate);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", (String) row[0]);
                order.put("orderNumber", (String) row[1]);
                order.put("orderDate", row[2]);
                order.put("date", row[2]);
                order.put("deadline", (String) row[3]);
                order.put("cardCount", ((Number) row[4]).intValue());
                order.put("nombreCartes", ((Number) row[4]).intValue());
                order.put("priority", (String) row[5]);
                order.put("status", row[6]);
                order.put("totalPrice", row[7]);


                orders.add(order);
            }

            log.info(" {} orders loaded for planning (excluding already planned)", orders.size());

            // Debug stats if no orders found
            if (orders.isEmpty()) {
                String countSql = "SELECT COUNT(*) FROM `order` o WHERE o.date >= ? AND o.status IN (1, 2)";
                Query countQuery = entityManager.createNativeQuery(countSql);
                countQuery.setParameter(1, fromDate);
                Number totalOrders = (Number) countQuery.getSingleResult();

                String plannedSql = "SELECT COUNT(DISTINCT jp.order_id) FROM j_planning jp JOIN `order` o ON jp.order_id = o.id WHERE o.date >= ?";
                Query plannedQuery = entityManager.createNativeQuery(plannedSql);
                plannedQuery.setParameter(1, fromDate);
                Number plannedOrders = (Number) plannedQuery.getSingleResult();

                log.info(" Orders stats: Total={}, Planned={}, Remaining={}",
                        totalOrders, plannedOrders, totalOrders.intValue() - plannedOrders.intValue());
            }

            return orders;

        } catch (Exception e) {
            log.error(" Error loading orders for planning: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    /**
     * Get count of already planned orders for diagnostics
     * @return count of orders that already have planning entries
     */
    @Transactional(readOnly = true)
    public long getAlreadyPlannedOrdersCount() {
        try {
            String sql = "SELECT COUNT(DISTINCT jp.order_id) FROM j_planning jp";
            Query query = entityManager.createNativeQuery(sql);
            return ((Number) query.getSingleResult()).longValue();
        } catch (Exception e) {
            log.error("Error counting planned orders: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * Check if a specific order is already planned
     * @param orderId the order ID to check
     * @return true if order is already planned
     */
    @Transactional(readOnly = true)
    public boolean isOrderAlreadyPlanned(String orderId) {
        try {
            String sql = "SELECT COUNT(*) FROM j_planning WHERE HEX(order_id) = ?";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, orderId.replace("-", ""));
            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            log.error("Error checking if order is planned: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get all orders as map for compatibility
     * @return list of orders as maps
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllOrdersAsMap() {
        try {
            List<Order> orders = orderRepository.findAll();
            List<Map<String, Object>> result = new ArrayList<>();

            for (Order order : orders) {
                Map<String, Object> orderMap = convertOrderToMap(order);
                result.add(orderMap);
            }

            return result;

        } catch (Exception e) {
            log.error("Error retrieving all orders", e);
            return new ArrayList<>();
        }
    }

    // ========== VALIDATION METHODS ==========

    /**
     * Validate new order business rules
     */
    private void validateNewOrder(Order order) {
        if (order.getCardCount() != null) {
            if (order.getCardCount() < MIN_CARDS_PER_ORDER || order.getCardCount() > MAX_CARDS_PER_ORDER) {
                throw new IllegalArgumentException(
                        String.format("Card count must be between %d and %d", MIN_CARDS_PER_ORDER, MAX_CARDS_PER_ORDER)
                );
            }
        }

        if (order.getOrderNumber() != null && orderRepository.findByOrderNumber(order.getOrderNumber()).isPresent()) {
            throw new IllegalArgumentException("Order number already exists: " + order.getOrderNumber());
        }
    }



    // ========== SEARCH AND FILTERING ==========

    /**
     * Search orders by various criteria
     * @param searchTerm search term
     * @param status order status filter
     * @param priority priority filter
     * @return list of filtered orders
     */
    @Transactional(readOnly = true)
    public List<Order> searchOrders(String searchTerm, Integer status, Order.OrderPriority priority) {
        return orderRepository.findAll().stream()
                .filter(order -> searchTerm == null ||
                        order.getOrderNumber().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        order.getCustomerName().toLowerCase().contains(searchTerm.toLowerCase()))
                .filter(order -> status == null || order.getStatus().equals(status))
                .filter(order -> priority == null || order.getPriority().equals(priority))
                .collect(Collectors.toList());
    }
    // ========== STATISTICS METHODS ==========

    /**
     * Get order statistics
     * @return statistics map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Basic counts
            long totalOrders = orderRepository.count();
            long pendingCount = orderRepository.countByStatus(Order.OrderStatus.PENDING);
            long inProgressCount = orderRepository.countByStatus(Order.OrderStatus.IN_PROGRESS);
            long completedCount = orderRepository.countByStatus(Order.OrderStatus.COMPLETED);

            stats.put("totalOrders", totalOrders);
            stats.put("pendingOrders", pendingCount);
            stats.put("inProgressOrders", inProgressCount);
            stats.put("completedOrders", completedCount);

            // Calculate completion rate
            if (totalOrders > 0) {
                double completionRate = (double) completedCount / totalOrders * 100;
                stats.put("completionRatePercent", Math.round(completionRate * 100.0) / 100.0);
            } else {
                stats.put("completionRatePercent", 0.0);
            }

            stats.put("success", true);
            stats.put("timestamp", LocalDateTime.now());

        } catch (Exception e) {
            log.error("Error calculating order statistics", e);
            stats.put("success", false);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // ========== UTILITY METHODS ==========


    /**
     * Calculate deadline label based on priority
     */
    private String calculateDeadlineLabel(Order order) {
        if (order.getPriority() == Order.OrderPriority.EXCELSIOR) {
            return "X"; // Most urgent (add this new case)
        } else if (order.getPriority() == Order.OrderPriority.FAST_PLUS) {
            return "F+"; // Urgent
        } else if (order.getPriority() == Order.OrderPriority.FAST) {
            return "F"; // Medium
        } else if (order.getPriority() == Order.OrderPriority.CLASSIC) {
            return "C"; // Normal
        } else {
            return "C"; // Default fallback
        }
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrdersAsMap() {
        try {
            log.info("🔍 Getting recent orders with REAL card counts from database");

            // SQL qui compte les vraies cartes depuis card_certification_order
            String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                o.num_commande_client as clientOrderNumber,
                DATE(o.date) as creationDate,
                o.date as fullTimestamp,
                o.status,
                COALESCE(o.priority_string, 'MEDIUM') as priority,
                COALESCE(o.temps_estime_minutes, 0) as estimatedTimeMinutes,
                COALESCE(o.prix_total, 0) as totalPrice,
                -- VRAI NOMBRE DE CARTES depuis la table de jointure
                COALESCE(
                    (SELECT COUNT(*) 
                     FROM card_certification_order cco 
                     WHERE cco.order_id = o.id), 
                    0
                ) as cardCount,
                -- VRAIES CARTES AVEC NOM
                COALESCE(
                    (SELECT COUNT(*) 
                     FROM card_certification_order cco 
                     INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
                     LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
                     WHERE cco.order_id = o.id 
                     AND (ct.name IS NOT NULL AND ct.name != '' AND ct.name != 'NULL')), 
                    0
                ) as cardsWithName,
                o.customer_id,
                o.reference,
                o.langue as language_code,
                o.special_grades as hasSpecialGrades
            FROM `order` o 
            WHERE o.date >= '2025-06-01' 
              AND o.annulee = 0
            ORDER BY o.date DESC
            LIMIT 1000
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();

                // Basic info
                order.put("id", row[0]);
                order.put("orderNumber", row[1] != null ? row[1] : "ORD-" + row[0]);
                order.put("clientOrderNumber", row[2]);
                order.put("creationDate", row[3]);
                order.put("fullTimestamp", row[4]);

                // Status mapping
                Number statusNum = (Number) row[5];
                order.put("status", mapStatusToText(statusNum));

                // Priority
                String priority = (String) row[6];
                order.put("priority", priority != null ? priority.toUpperCase() : "MEDIUM");

                // Times and price
                order.put("estimatedTimeMinutes", ((Number) row[7]).intValue());
                order.put("totalPrice", ((Number) row[8]).doubleValue());

                // VRAIES DONNÉES DE CARTES (plus de hardcoding à 25!)
                Number cardCount = (Number) row[9];
                Number cardsWithName = (Number) row[10];

                order.put("cardCount", cardCount.intValue());
                order.put("cardsWithName", cardsWithName.intValue());

                // Calculate real percentage
                double namePercentage = cardCount.intValue() > 0
                        ? Math.round((cardsWithName.doubleValue() / cardCount.doubleValue()) * 100)
                        : 0;
                order.put("namePercentage", namePercentage);

                // Recalculate estimated duration based on real card count
                int realDuration = cardCount.intValue() * 3; // 3 minutes per card
                order.put("estimatedDuration", realDuration);

                // Additional info
                order.put("customerId", row[11]);
                order.put("reference", row[12]);
                order.put("languageCode", row[13]);
                order.put("hasSpecialGrades", row[14]);

                orders.add(order);
            }

            log.info("✅ Retrieved {} orders with real card counts", orders.size());

            // Debug: Log first few orders to verify
            if (!orders.isEmpty()) {
                Map<String, Object> firstOrder = orders.get(0);
                log.info("📊 Sample order: {} - {} cards ({}% with names)",
                        firstOrder.get("orderNumber"),
                        firstOrder.get("cardCount"),
                        firstOrder.get("namePercentage"));
            }

            return orders;

        } catch (Exception e) {
            log.error("❌ Error getting recent orders", e);
            return new ArrayList<>();
        }
    }

    // ========== Méthode helper pour mapper le status ==========
    private String mapStatusToText(Number statusNum) {
        if (statusNum == null) return "PENDING";

        int status = statusNum.intValue();
        return switch (status) {
            case 0 -> "PENDING";
            case 1 -> "SCHEDULED";
            case 2 -> "IN_PROGRESS";
            case 3 -> "COMPLETED";
            case 4 -> "CANCELLED";
            default -> "PENDING";
        };
    }

    // ========== COMPATIBILITY METHODS (for migration from CommandeService) ==========

    /**
     * Legacy method for compatibility with existing code
     * @deprecated Use getAllOrdersAsMap() instead
     */
    @Deprecated
    public List<Map<String, Object>> getToutesCommandes() {
        log.warn("Using deprecated method getToutesCommandes(), please use getAllOrdersAsMap()");
        return getAllOrdersAsMap();
    }

    /**
     * Legacy method for compatibility with existing code
     * @deprecated Use getOrdersForPlanning() instead
     */
    @Deprecated
    public List<Map<String, Object>> getCommandesDepuis(int jour, int mois, int annee) {
        log.warn("Using deprecated method getCommandesDepuis(), please use getOrdersForPlanning()");
        return getOrdersForPlanning(jour, mois, annee);
    }

    /**
     * Get recent orders with REAL card counts from database
     * Fixed version that calculates actual card counts from card_certification_order table
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentOrders() {
        log.info("🔍 Getting recent orders with REAL card counts from database");

        String sql = """
        SELECT
            HEX(o.id) as id,
            o.num_commande as orderNumber,
            o.num_commande_client as clientOrderNumber,
            DATE(o.date) as creationDate,
            o.date as fullTimestamp,
            o.status,
            'MEDIUM' as priority,
            0 as estimatedTimeMinutes,
            0 as totalPrice,
            -- ✅ VRAI NOMBRE DE CARTES depuis la table de jointure
            COALESCE(
                (SELECT COUNT(*)
                 FROM card_certification_order cco
                 WHERE cco.order_id = o.id),
                0
            ) as cardCount,
            -- ✅ VRAIES CARTES AVEC NOM
            COALESCE(
                (SELECT COUNT(*)
                 FROM card_certification_order cco
                 INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
                 LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'fr'
                 WHERE cco.order_id = o.id
                 AND (ct.name IS NOT NULL AND ct.name != '' AND ct.name != 'NULL')),
                0
            ) as cardsWithName,
            o.customer_id,
            o.reference,
            'fr' as language_code,
            false as hasSpecialGrades
        FROM `order` o
        WHERE o.status NOT IN (5, 8)  -- Exclure ENVOYEE et RECU
          AND o.annulee = 0
        ORDER BY o.date DESC
        LIMIT 1000
        """;

        try {
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            log.info("✅ Found {} orders", results.size());

            return results.stream()
                    .map(this::mapRowToOrderMap)
                    .toList();

        } catch (Exception e) {
            log.error("❌ Error getting recent orders", e);
            throw new RuntimeException("Failed to get orders: " + e.getMessage(), e);
        }
    }

    /**
     * Map database row to order map
     */
    private Map<String, Object> mapRowToOrderMap(Object[] row) {
        Map<String, Object> orderMap = new LinkedHashMap<>();
        int i = 0;

        orderMap.put("id", row[i++]);
        orderMap.put("orderNumber", row[i++]);
        orderMap.put("clientOrderNumber", row[i++]);
        orderMap.put("creationDate", row[i++]);
        orderMap.put("fullTimestamp", row[i++]);
        orderMap.put("status", row[i++]);
        orderMap.put("priority", row[i++]);
        orderMap.put("estimatedTimeMinutes", ((Number) row[i++]).intValue());
        orderMap.put("totalPrice", row[i++] != null ? ((Number) row[i - 1]).doubleValue() : 0.0);
        orderMap.put("cardCount", ((Number) row[i++]).intValue());        // ✅ VRAI nombre
        orderMap.put("cardsWithName", ((Number) row[i++]).intValue());    // ✅ VRAI nombre
        orderMap.put("customerId", row[i++]);
        orderMap.put("reference", row[i++]);
        orderMap.put("languageCode", row[i++]);
        orderMap.put("hasSpecialGrades", row[i++]);

        return orderMap;
    }

}