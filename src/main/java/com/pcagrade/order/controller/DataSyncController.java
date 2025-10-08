package com.pcagrade.order.controller;

import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.pcagrade.order.service.SymfonyApiClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    @Lazy
    private DataSyncController self;

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
    public ResponseEntity<Map<String, Object>> syncAll() {
        Map<String, Object> result = new HashMap<>();
        List<String> syncedTables = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        try {
            log.info("🔄 Starting full synchronization from Symfony API");

            if (!symfonyApiClient.isHealthy()) {
                result.put("success", false);
                result.put("message", "❌ Symfony API is not accessible");
                return ResponseEntity.status(503).body(result);
            }

            // Skip Orders and CardTranslations - use SQL sync instead
            skipped.add("Orders (use SQL sync: mysql -u ia -p < sync_dev_to_planning.sql)");
            skipped.add("CardTranslations (use SQL sync)");

            try {
                syncedTables.add(self.syncCards());
            } catch (Exception e) {
                log.error("❌ Error syncing cards", e);
                errors.add("Cards: " + e.getMessage());
            }

            try {
                syncedTables.add(self.syncCardCertifications());
            } catch (Exception e) {
                log.error("❌ Error syncing card certifications", e);
                errors.add("CardCertifications: " + e.getMessage());
            }

            try {
                syncedTables.add(self.syncCardCertificationOrders());
            } catch (Exception e) {
                log.error("❌ Error syncing card-order links", e);
                errors.add("CardCertificationOrders: " + e.getMessage());
            }

            try {
                syncedTables.add(self.syncInvoices());
            } catch (Exception e) {
                log.error("❌ Error syncing invoices", e);
                errors.add("Invoices: " + e.getMessage());
            }

            boolean hasErrors = !errors.isEmpty();
            result.put("success", !hasErrors);
            result.put("message", hasErrors ?
                    "⚠️ Sync completed with errors" :
                    "✅ API synchronization completed (Orders & CardTranslations via SQL)");
            result.put("syncedTables", syncedTables);
            result.put("skippedTables", skipped);
            if (hasErrors) {
                result.put("errors", errors);
            }
            result.put("timestamp", new Date());

            log.info("✅ API sync completed. {} tables synced, {} skipped",
                    syncedTables.size(), skipped.size());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Critical error during full synchronization", e);
            result.put("success", false);
            result.put("message", "Synchronization failed: " + e.getMessage());
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
    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {RuntimeException.class})
    protected String syncOrders() {
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String syncCards() {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = {RuntimeException.class})
    protected String syncCardTranslations() {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String syncCardCertifications() {
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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String syncCardCertificationOrders() {
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



    // ===============================================
// ADD THESE METHODS TO DataSyncController.java
// ===============================================

    /**
     * Sync only invoices table
     */
    @PostMapping("/invoices")
    @Transactional
    public ResponseEntity<Map<String, Object>> syncInvoicesEndpoint() {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🔄 Syncing invoices from Symfony API");
            String syncResult = syncInvoices();

            result.put("success", true);
            result.put("message", syncResult);
            result.put("timestamp", new Date());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Error syncing invoices", e);
            result.put("success", false);
            result.put("message", "Sync failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    // ========== ADD TO syncAll() METHOD ==========
    // Add this line after syncCardCertificationOrders():
    // syncedTables.add(self.syncInvoices());

    // ========== PRIVATE SYNC METHOD ==========

    /**
     * Sync invoices from Symfony API
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected String syncInvoices() {
        log.info("📄 Starting invoice synchronization...");

        try {
            List<Map<String, Object>> invoices = symfonyApiClient.fetchInvoicesForActiveOrders();
            log.info("📦 Fetched {} invoices from Symfony API", invoices.size());

            if (invoices.isEmpty()) {
                log.warn("⚠️ No invoices found in Symfony API");
                return "⚠️ No invoices to sync";
            }

            int inserted = 0;
            int updated = 0;
            int errors = 0;

            for (Map<String, Object> invoice : invoices) {
                try {
                    String id = (String) invoice.get("id");
                    String orderId = (String) invoice.get("order_id");

                    if (id == null || orderId == null) {
                        log.warn("⚠️ Skipping invoice with missing ID or order_id");
                        errors++;
                        continue;
                    }

                    byte[] idBinary = hexStringToByteArray(id);
                    byte[] orderIdBinary = hexStringToByteArray(orderId);

                    // Check if invoice exists
                    Query checkQuery = entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM invoice WHERE id = :id"
                    );
                    checkQuery.setParameter("id", idBinary);
                    Long count = ((Number) checkQuery.getSingleResult()).longValue();

                    if (count > 0) {
                        // Update existing invoice - using REAL columns
                        Query updateQuery = entityManager.createNativeQuery(
                                """
                                UPDATE invoice SET
                                    order_id = :orderId,
                                    total_ht = :totalHt,
                                    total_ttc = :totalTtc,
                                    montant_tva = :montantTva
                                WHERE id = :id
                                """
                        );
                        updateQuery.setParameter("id", idBinary);
                        updateQuery.setParameter("orderId", orderIdBinary);
                        updateQuery.setParameter("totalHt", getDoubleValue(invoice, "total_ht"));
                        updateQuery.setParameter("totalTtc", getDoubleValue(invoice, "total_ttc"));
                        updateQuery.setParameter("montantTva", getDoubleValue(invoice, "montant_tva"));
                        updateQuery.executeUpdate();
                        updated++;

                    } else {
                        // Insert new invoice - using REAL columns
                        Query insertQuery = entityManager.createNativeQuery(
                                """
                                INSERT INTO invoice (
                                    id, order_id, ref,
                                    total_ht, total_ttc, montant_tva,
                                    nb_can, nb_csn,
                                    livraison_ht, livraison_ttc,
                                    total_articles_ht, total_articles_ttc,
                                    CA, assurance_ttc, assurance_ht,
                                    remise_ht, remise_ttc,
                                    date, payee,
                                    taux_tva, ref_bis, delai,
                                    cout_protection_ht, cout_emballage_ht,
                                    carte_cadeau,
                                    paypal_amount_ttc, paypal_amount_ht,
                                    montant_tvaassurance, montant_tvanormale
                                ) VALUES (
                                    :id, :orderId, :ref,
                                    :totalHt, :totalTtc, :montantTva,
                                    :nbCan, :nbCsn,
                                    :livraisonHt, :livraisonTtc,
                                    :totalArticlesHt, :totalArticlesTtc,
                                    :ca, :assuranceTtc, :assuranceHt,
                                    :remiseHt, :remiseTtc,
                                    :date, :payee,
                                    :tauxTva, :refBis, :delai,
                                    :coutProtectionHt, :coutEmballageHt,
                                    :carteCadeau,
                                    :paypalAmountTtc, :paypalAmountHt,
                                    :montantTvaassurance, :montantTvanormale
                                )
                                """
                        );

                        // Set all parameters with safe defaults
                        insertQuery.setParameter("id", idBinary);
                        insertQuery.setParameter("orderId", orderIdBinary);
                        insertQuery.setParameter("ref", getIntValue(invoice, "ref", 0));

                        insertQuery.setParameter("totalHt", getDoubleValue(invoice, "total_ht"));
                        insertQuery.setParameter("totalTtc", getDoubleValue(invoice, "total_ttc"));
                        insertQuery.setParameter("montantTva", getDoubleValue(invoice, "montant_tva"));

                        insertQuery.setParameter("nbCan", getIntValue(invoice, "nb_can", 0));
                        insertQuery.setParameter("nbCsn", getIntValue(invoice, "nb_csn", 0));

                        insertQuery.setParameter("livraisonHt", getDoubleValue(invoice, "livraison_ht", 0.0));
                        insertQuery.setParameter("livraisonTtc", getDoubleValue(invoice, "livraison_ttc", 0.0));

                        insertQuery.setParameter("totalArticlesHt", getDoubleValue(invoice, "total_articles_ht", 0.0));
                        insertQuery.setParameter("totalArticlesTtc", getDoubleValue(invoice, "total_articles_ttc", 0.0));

                        insertQuery.setParameter("ca", getDoubleValue(invoice, "CA", 0.0));
                        insertQuery.setParameter("assuranceTtc", getDoubleValue(invoice, "assurance_ttc", 0.0));
                        insertQuery.setParameter("assuranceHt", getDoubleValue(invoice, "assurance_ht", 0.0));

                        insertQuery.setParameter("remiseHt", getDoubleValue(invoice, "remise_ht", 0.0));
                        insertQuery.setParameter("remiseTtc", getDoubleValue(invoice, "remise_ttc", 0.0));

                        insertQuery.setParameter("date", invoice.get("date"));
                        insertQuery.setParameter("payee", getBooleanValue(invoice, "payee", false));

                        insertQuery.setParameter("tauxTva", getDoubleValue(invoice, "taux_tva", 0.0));
                        insertQuery.setParameter("refBis", invoice.getOrDefault("ref_bis", ""));
                        insertQuery.setParameter("delai", invoice.get("delai"));

                        insertQuery.setParameter("coutProtectionHt", getDoubleValue(invoice, "cout_protection_ht", 0.0));
                        insertQuery.setParameter("coutEmballageHt", getDoubleValue(invoice, "cout_emballage_ht", 0.0));

                        insertQuery.setParameter("carteCadeau", getBooleanValue(invoice, "carte_cadeau", false));

                        insertQuery.setParameter("paypalAmountTtc", getDoubleValue(invoice, "paypal_amount_ttc", 0.0));
                        insertQuery.setParameter("paypalAmountHt", getDoubleValue(invoice, "paypal_amount_ht", 0.0));

                        insertQuery.setParameter("montantTvaassurance", getDoubleValue(invoice, "montant_tvaassurance", 0.0));
                        insertQuery.setParameter("montantTvanormale", getDoubleValue(invoice, "montant_tvanormale", 0.0));

                        insertQuery.executeUpdate();
                        inserted++;
                    }

                } catch (Exception e) {
                    log.error("❌ Error processing invoice: " + invoice.get("id"), e);
                    errors++;
                }
            }

            entityManager.flush();

            String result = String.format(
                    "✅ Invoices synced: %d inserted, %d updated, %d errors",
                    inserted, updated, errors
            );
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("❌ Critical error during invoice synchronization", e);
            throw new RuntimeException("Failed to sync invoices: " + e.getMessage(), e);
        }
    }

    // Helper methods to safely extract values
    private double getDoubleValue(Map<String, Object> map, String key) {
        return getDoubleValue(map, key, 0.0);
    }

    private double getDoubleValue(Map<String, Object> map, String key, double defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    // ========== UPDATE getSyncStatus() METHOD ==========
    // Add this line to the tableComparison list:
    // tableComparison.add(compareTableWithSymfony("invoice",
    //         symfonyApiClient.countInvoices()));

    // ========== HELPER METHOD (if not already present) ==========

    /**
     * Convert hex string to byte array
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * Compare local table count with Symfony count
     */
    private Map<String, Object> compareTableWithSymfony(String tableName, int symfonyCount) {
        Map<String, Object> comparison = new HashMap<>();

        try {
            // Échapper le nom de table avec des backticks pour les mots réservés SQL
            String sql = "SELECT COUNT(*) FROM `" + tableName + "`";
            Query query = entityManager.createNativeQuery(sql);
            Long localCount = ((Number) query.getSingleResult()).longValue();

            comparison.put("table", tableName);
            comparison.put("local", localCount);
            comparison.put("symfony", symfonyCount);
            comparison.put("difference", Math.abs(localCount - symfonyCount));
            comparison.put("inSync", localCount.intValue() == symfonyCount);

        } catch (Exception e) {
            log.error("❌ Error comparing table: " + tableName, e);
            comparison.put("table", tableName);
            comparison.put("error", e.getMessage());
            comparison.put("inSync", false);
        }

        return comparison;
    }
    /**
     * Get nested integer value from map
     */
    private int getNestedInt(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return 0;
            }
        }
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return 0;
    }

}