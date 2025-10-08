package com.pcagrade.order.controller;

import com.pcagrade.order.service.SymfonyApiClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Data Sync Controller - API Version
 * Synchronizes data from Symfony API to dev-planning database
 */
@RestController
@RequestMapping("/api/sync")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class DataSyncController {

    private static final Logger log = LoggerFactory.getLogger(DataSyncController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SymfonyApiClient symfonyApiClient;

    /**
     * Check Symfony API health
     */
    @GetMapping("/symfony-health")
    public ResponseEntity<Map<String, Object>> checkSymfonyHealth() {
        Map<String, Object> result = new HashMap<>();

        try {
            boolean healthy = symfonyApiClient.isHealthy();

            result.put("success", healthy);
            result.put("status", healthy ? "healthy" : "unhealthy");
            result.put("message", healthy ? "✅ Symfony API is accessible" : "⚠️ Cannot reach Symfony API");
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error checking Symfony health", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Get statistics from Symfony API
     */
    @GetMapping("/symfony-stats")
    public ResponseEntity<Map<String, Object>> getSymfonyStats() {
        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> stats = symfonyApiClient.getStats();

            result.put("success", true);
            result.put("data", stats);
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error fetching Symfony stats", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Sync all tables from Symfony API
     */
    @PostMapping("/all")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> result = new HashMap<>();
        List<String> syncedTables = new ArrayList<>();

        try {
            log.info("🔄 Starting full synchronization from Symfony API");

            // Check Symfony API health first
            if (!symfonyApiClient.isHealthy()) {
                result.put("success", false);
                result.put("message", "❌ Symfony API is not accessible");
                return ResponseEntity.status(503).body(result);
            }

            // Sync each table
            syncedTables.add(syncOrders());
            syncedTables.add(syncCards());
            syncedTables.add(syncCardTranslations());
            syncedTables.add(syncCardCertifications());
            syncedTables.add(syncCardCertificationOrders());

            result.put("success", true);
            result.put("message", "✅ Full synchronization completed");
            result.put("syncedTables", syncedTables);
            result.put("timestamp", new Date());

            log.info("✅ Full synchronization completed successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error during full synchronization", e);
            result.put("success", false);
            result.put("message", "Synchronization failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Sync only orders table
     */
    @PostMapping("/orders")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncOrdersEndpoint() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🔄 Syncing orders from Symfony API");
            String syncResult = syncOrders();

            result.put("success", true);
            result.put("message", syncResult);
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Sync only cards table
     */
    @PostMapping("/cards")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncCardsEndpoint() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🔄 Syncing cards from Symfony API");

            syncCards();
            syncCardTranslations();
            syncCardCertifications();
            syncCardCertificationOrders();

            result.put("success", true);
            result.put("message", "✅ Cards synchronized successfully");
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * Get sync status - compare Symfony vs dev-planning
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("📊 Checking sync status");

            // Get stats from Symfony
            Map<String, Object> symfonyStats = symfonyApiClient.getStats();

            // Get local counts
            List<Map<String, Object>> tableComparison = new ArrayList<>();

            tableComparison.add(compareTableWithSymfony("order",
                    getNestedInt(symfonyStats, "orders", "active")));
            tableComparison.add(compareTableWithSymfony("card",
                    getNestedInt(symfonyStats, "cards", "total")));
            tableComparison.add(compareTableWithSymfony("card_translation",
                    getNestedInt(symfonyStats, "cardTranslations", "total")));
            tableComparison.add(compareTableWithSymfony("card_certification",
                    getNestedInt(symfonyStats, "cardCertifications", "total")));

            result.put("success", true);
            result.put("tableComparison", tableComparison);
            result.put("symfonyStats", symfonyStats);
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error checking sync status", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ========== PRIVATE SYNC METHODS ==========

    private String syncOrders() {
        try {
            log.info("🔄 Syncing orders from Symfony API...");

            List<Map<String, Object>> orders = symfonyApiClient.fetchAllOrders();

            if (orders.isEmpty()) {
                log.warn("⚠️ No orders found in Symfony API");
                return "Orders: 0 fetched, 0 synced";
            }

            // Delete existing
            String deleteSql = "DELETE FROM `order`";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deleted = deleteQuery.executeUpdate();

            // Bulk insert would be better, but for simplicity:
            int inserted = 0;
            for (Map<String, Object> order : orders) {
                try {
                    insertOrder(order);
                    inserted++;
                } catch (Exception e) {
                    log.error("❌ Error inserting order: {}", order.get("id"), e);
                }
            }

            String message = String.format("Orders: %d deleted, %d inserted", deleted, inserted);
            log.info("✅ {}", message);
            return message;

        } catch (Exception e) {
            log.error("❌ Error syncing orders", e);
            throw new RuntimeException("Failed to sync orders: " + e.getMessage(), e);
        }
    }

    private String syncCards() {
        try {
            log.info("🔄 Syncing cards from Symfony API...");

            List<Map<String, Object>> cards = symfonyApiClient.fetchAllCards();

            String deleteSql = "DELETE FROM `card`";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deleted = deleteQuery.executeUpdate();

            int inserted = 0;
            for (Map<String, Object> card : cards) {
                try {
                    insertCard(card);
                    inserted++;
                } catch (Exception e) {
                    log.error("❌ Error inserting card: {}", card.get("id"), e);
                }
            }

            String message = String.format("Cards: %d deleted, %d inserted", deleted, inserted);
            log.info("✅ {}", message);
            return message;

        } catch (Exception e) {
            log.error("❌ Error syncing cards", e);
            throw new RuntimeException("Failed to sync cards: " + e.getMessage(), e);
        }
    }

    private String syncCardTranslations() {
        try {
            log.info("🔄 Syncing card translations from Symfony API...");

            List<Map<String, Object>> translations = symfonyApiClient.fetchAllCardTranslations();

            String deleteSql = "DELETE FROM `card_translation`";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deleted = deleteQuery.executeUpdate();

            int inserted = 0;
            for (Map<String, Object> translation : translations) {
                try {
                    insertCardTranslation(translation);
                    inserted++;
                } catch (Exception e) {
                    log.error("❌ Error inserting translation: {}", translation.get("id"), e);
                }
            }

            String message = String.format("Translations: %d deleted, %d inserted", deleted, inserted);
            log.info("✅ {}", message);
            return message;

        } catch (Exception e) {
            log.error("❌ Error syncing translations", e);
            throw new RuntimeException("Failed to sync translations: " + e.getMessage(), e);
        }
    }

    private String syncCardCertifications() {
        try {
            log.info("🔄 Syncing card certifications from Symfony API...");

            List<Map<String, Object>> certifications = symfonyApiClient.fetchAllCardCertifications();

            String deleteSql = "DELETE FROM `card_certification`";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deleted = deleteQuery.executeUpdate();

            int inserted = 0;
            for (Map<String, Object> cert : certifications) {
                try {
                    insertCardCertification(cert);
                    inserted++;
                } catch (Exception e) {
                    log.error("❌ Error inserting certification: {}", cert.get("id"), e);
                }
            }

            String message = String.format("Certifications: %d deleted, %d inserted", deleted, inserted);
            log.info("✅ {}", message);
            return message;

        } catch (Exception e) {
            log.error("❌ Error syncing certifications", e);
            throw new RuntimeException("Failed to sync certifications: " + e.getMessage(), e);
        }
    }

    private String syncCardCertificationOrders() {
        try {
            log.info("🔄 Syncing card certification orders from Symfony API...");

            List<Map<String, Object>> certOrders = symfonyApiClient.fetchAllCardCertificationOrders();

            String deleteSql = "DELETE FROM `card_certification_order`";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            int deleted = deleteQuery.executeUpdate();

            int inserted = 0;
            for (Map<String, Object> certOrder : certOrders) {
                try {
                    insertCardCertificationOrder(certOrder);
                    inserted++;
                } catch (Exception e) {
                    log.error("❌ Error inserting cert order", e);
                }
            }

            String message = String.format("Cert Orders: %d deleted, %d inserted", deleted, inserted);
            log.info("✅ {}", message);
            return message;

        } catch (Exception e) {
            log.error("❌ Error syncing cert orders", e);
            throw new RuntimeException("Failed to sync cert orders: " + e.getMessage(), e);
        }
    }

    // ========== INSERT METHODS ==========

    private void insertOrder(Map<String, Object> order) {
        // Simplified - adjust columns based on your actual schema
        String sql = """
            INSERT INTO `order` (
                id, num_commande, date, status, delai, annulee, paused
            ) VALUES (
                UNHEX(?), ?, ?, ?, ?, ?, ?
            )
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, order.get("id"));
        query.setParameter(2, order.get("num_commande"));
        query.setParameter(3, order.get("date"));
        query.setParameter(4, order.get("status"));
        query.setParameter(5, order.get("delai"));
        query.setParameter(6, order.get("annulee"));
        query.setParameter(7, order.get("paused"));

        query.executeUpdate();
    }

    private void insertCard(Map<String, Object> card) {
        String sql = "INSERT INTO `card` (id) VALUES (UNHEX(?))";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, card.get("id"));
        query.executeUpdate();
    }

    private void insertCardTranslation(Map<String, Object> translation) {
        String sql = """
            INSERT INTO `card_translation` (
                id, translatable_id, name, locale
            ) VALUES (
                UNHEX(?), UNHEX(?), ?, ?
            )
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, translation.get("id"));
        query.setParameter(2, translation.get("translatable_id"));
        query.setParameter(3, translation.get("name"));
        query.setParameter(4, translation.get("locale"));
        query.executeUpdate();
    }

    private void insertCardCertification(Map<String, Object> cert) {
        String sql = """
            INSERT INTO `card_certification` (
                id, card_id, code_barre
            ) VALUES (
                UNHEX(?), UNHEX(?), ?
            )
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, cert.get("id"));
        query.setParameter(2, cert.get("card_id"));
        query.setParameter(3, cert.get("code_barre"));
        query.executeUpdate();
    }

    private void insertCardCertificationOrder(Map<String, Object> certOrder) {
        String sql = """
            INSERT INTO `card_certification_order` (
                order_id, card_certification_id
            ) VALUES (
                UNHEX(?), UNHEX(?)
            )
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, certOrder.get("order_id"));
        query.setParameter(2, certOrder.get("card_certification_id"));
        query.executeUpdate();
    }

    // ========== HELPER METHODS ==========

    private Map<String, Object> compareTableWithSymfony(String tableName, int symfonyCount) {
        Map<String, Object> comparison = new HashMap<>();
        comparison.put("table", tableName);

        try {
            String sql = String.format("SELECT COUNT(*) FROM `%s`", tableName);
            Query query = entityManager.createNativeQuery(sql);
            Number localCount = (Number) query.getSingleResult();

            comparison.put("symfonyCount", symfonyCount);
            comparison.put("devPlanningCount", localCount.intValue());
            comparison.put("difference", symfonyCount - localCount.intValue());
            comparison.put("inSync", symfonyCount == localCount.intValue());

        } catch (Exception e) {
            comparison.put("error", e.getMessage());
            comparison.put("inSync", false);
        }

        return comparison;
    }

    private int getNestedInt(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return 0;
            }
        }
        return current != null ? ((Number) current).intValue() : 0;
    }
}