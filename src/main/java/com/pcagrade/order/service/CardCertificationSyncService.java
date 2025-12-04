package com.pcagrade.order.service;

import com.pcagrade.order.entity.Order;
import com.pcagrade.order.repository.OrderRepository;
import com.pcagrade.order.util.UlidConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ULTRA-FAST Card Certification Sync Service using native JDBC batch
 *
 * UPDATED: Added support for new GptOrderController API format
 *
 * Uses raw JDBC INSERT instead of JPA for maximum performance
 * Expected: 50,000 cards in ~30-60 seconds
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CardCertificationSyncService {

    private final OrderRepository orderRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final int BATCH_SIZE = 1000;

    /**
     * Sync cards from GptOrderController API format
     * NEW METHOD: Handles card_certifications array from /api/gpt/orders response
     *
     * @param symfonyOrderId The Symfony order ID (parent order)
     * @param cardCertifications List of card certification data from API
     * @return Number of cards successfully synced
     */
    @Transactional
    public int syncCardsFromGptApi(String symfonyOrderId, List<Map<String, Object>> cardCertifications) {
        if (cardCertifications == null || cardCertifications.isEmpty()) {
            return 0;
        }

        // Find the order in our database
        Optional<Order> orderOpt = orderRepository.findBySymfonyOrderId(symfonyOrderId);
        if (orderOpt.isEmpty()) {
            log.warn(" Order not found for Symfony ID: {}, skipping {} cards",
                    symfonyOrderId, cardCertifications.size());
            return 0;
        }

        UUID orderId = orderOpt.get().getId();
        log.debug(" Syncing {} cards for order: {}", cardCertifications.size(), symfonyOrderId);

        String sql = "INSERT INTO card_certification (" +
                "id, order_id, card_name, code_barre, card_number, set_name, serie_name, " +
                "langue, declared_value, grade, grade_1, grade_2, grade_3, " +
                "grading_completed, certification_completed, scanning_completed, packaging_completed, " +
                "status, is_deleted, reverse, edition, shadowless, foil, csn, multi_grade, " +
                "creation_date, modification_date" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "card_name = VALUES(card_name), " +
                "code_barre = VALUES(code_barre), " +
                "card_number = VALUES(card_number), " +
                "set_name = VALUES(set_name), " +
                "serie_name = VALUES(serie_name), " +
                "langue = VALUES(langue), " +
                "declared_value = VALUES(declared_value), " +
                "grade = VALUES(grade), " +
                "grade_1 = VALUES(grade_1), " +
                "grade_2 = VALUES(grade_2), " +
                "grade_3 = VALUES(grade_3), " +
                "grading_completed = VALUES(grading_completed), " +
                "certification_completed = VALUES(certification_completed), " +
                "scanning_completed = VALUES(scanning_completed), " +
                "packaging_completed = VALUES(packaging_completed), " +
                "status = VALUES(status), " +
                "is_deleted = VALUES(is_deleted), " +
                "modification_date = VALUES(modification_date)";

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();
        int skipped = 0;

        for (Map<String, Object> cardData : cardCertifications) {
            try {
                // Get certification ID (required)
                String certIdStr = getString(cardData, "id");
                if (certIdStr == null || certIdStr.isEmpty()) {
                    skipped++;
                    continue;
                }

                UUID certificationId = UlidConverter.hexToUuid(certIdStr);

                // Map fields from new GptOrderController format
                String cardName = getString(cardData, "card_name");
                String barcode = getString(cardData, "barcode"); // NEW: was code_barre
                String cardNumber = getString(cardData, "card_number");
                String setName = getString(cardData, "set_name");
                String serieName = getString(cardData, "serie_name");
                String language = getString(cardData, "language"); // NEW: was langue
                Float declaredValue = getFloat(cardData, "declared_value");

                // Grades (new fields)
                String grade = getString(cardData, "grade");
                String grade1 = getString(cardData, "grade_1");
                String grade2 = getString(cardData, "grade_2");
                String grade3 = getString(cardData, "grade_3");

                // Status (integer from Symfony)
                Integer status = getInteger(cardData, "status");
                Boolean isDeleted = getBoolean(cardData, "is_deleted", false);

                // Additional fields
                String reverse = getString(cardData, "reverse");
                String edition = getString(cardData, "edition");
                String shadowless = getString(cardData, "shadowless");
                String foil = getString(cardData, "foil");
                String csn = getString(cardData, "csn");
                Boolean multiGrade = getBoolean(cardData, "multi_grade", false);

                // Determine completion flags from status
                // Symfony statuses for cards are similar to orders
                boolean gradingCompleted = determineGradingCompleted(status, grade);
                boolean certificationCompleted = determineCertificationCompleted(status);
                boolean scanningCompleted = determineScanningCompleted(status);
                boolean packagingCompleted = determinePackagingCompleted(status);

                // Use barcode or fallback to ID
                if (barcode == null || barcode.isEmpty()) {
                    barcode = certIdStr;
                }

                // Create batch arguments
                Object[] args = new Object[] {
                        uuidToBytes(certificationId),   // id
                        uuidToBytes(orderId),           // order_id
                        cardName,                       // card_name
                        barcode,                        // code_barre
                        cardNumber,                     // card_number
                        setName,                        // set_name
                        serieName,                      // serie_name
                        language != null ? language : "FR", // langue
                        declaredValue,                  // declared_value
                        grade,                          // grade
                        grade1,                         // grade_1
                        grade2,                         // grade_2
                        grade3,                         // grade_3
                        gradingCompleted,               // grading_completed
                        certificationCompleted,         // certification_completed
                        scanningCompleted,              // scanning_completed
                        packagingCompleted,             // packaging_completed
                        status != null ? status : 0,    // status
                        isDeleted,                      // is_deleted
                        reverse,                        // reverse
                        edition,                        // edition
                        shadowless,                     // shadowless
                        foil,                           // foil
                        csn,                            // csn
                        multiGrade,                     // multi_grade
                        now,                            // creation_date
                        now                             // modification_date
                };

                batchArgs.add(args);

            } catch (Exception e) {
                log.warn(" Error preparing card certification: {}", e.getMessage());
                skipped++;
            }
        }

        if (batchArgs.isEmpty()) {
            log.debug("No valid cards to sync for order: {}", symfonyOrderId);
            return 0;
        }

        // Execute batch insert
        int successCount = 0;
        try {
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
            for (int result : results) {
                if (result > 0 || result == -2) { // -2 = SUCCESS_NO_INFO
                    successCount++;
                }
            }
            log.debug(" Synced {}/{} cards for order {} ({} skipped)",
                    successCount, cardCertifications.size(), symfonyOrderId, skipped);
        } catch (Exception e) {
            log.error(" Batch insert failed for order {}: {}", symfonyOrderId, e.getMessage());
        }

        return successCount;
    }

    /**
     * Original sync method for old API format (PlanningExportController)
     * Kept for backward compatibility
     */
    @Transactional
    public int syncCards(List<Map<String, Object>> cardsData) {
        if (cardsData == null || cardsData.isEmpty()) {
            log.info("No cards to sync");
            return 0;
        }

        log.info(" Starting FAST JDBC card sync: {} cards to process", cardsData.size());
        long startTime = System.currentTimeMillis();

        int totalSuccess = 0;
        int totalErrors = 0;

        // Split into batches
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < cardsData.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, cardsData.size());
            batches.add(cardsData.subList(i, end));
        }

        log.info(" Processing {} batches of up to {} cards each", batches.size(), BATCH_SIZE);

        // Process each batch
        int batchNumber = 0;
        for (List<Map<String, Object>> batch : batches) {
            batchNumber++;
            try {
                int batchSuccess = insertBatchJdbcLegacy(batch, batchNumber);
                totalSuccess += batchSuccess;

                if (batchSuccess < batch.size()) {
                    totalErrors += (batch.size() - batchSuccess);
                }

                if (batchNumber % 10 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double rate = totalSuccess / (elapsed / 1000.0);
                    log.info(" Progress: {} batches, {}/{} cards ({:.0f} cards/sec)",
                            batchNumber, totalSuccess, cardsData.size(), rate);
                }

            } catch (Exception e) {
                log.error(" Batch {} failed: {}", batchNumber, e.getMessage());
                totalErrors += batch.size();
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        double rate = totalSuccess / (duration / 1000.0);

        log.info(" Card sync completed: {}/{} cards in {:.1f}s ({:.0f} cards/sec)",
                totalSuccess, cardsData.size(), duration / 1000.0, rate);

        return totalSuccess;
    }

    /**
     * Legacy batch insert for old API format
     */
    private int insertBatchJdbcLegacy(List<Map<String, Object>> batchData, int batchNumber) {
        String sql = "INSERT INTO card_certification (" +
                "id, order_id, card_name, code_barre, " +
                "grading_completed, certification_completed, " +
                "scanning_completed, packaging_completed, " +
                "date, status, langue, creation_date, modification_date" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "card_name = VALUES(card_name), " +
                "code_barre = VALUES(code_barre), " +
                "grading_completed = VALUES(grading_completed), " +
                "certification_completed = VALUES(certification_completed), " +
                "scanning_completed = VALUES(scanning_completed), " +
                "packaging_completed = VALUES(packaging_completed), " +
                "modification_date = VALUES(modification_date)";

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        for (Map<String, Object> cardData : batchData) {
            try {
                String certificationIdHex = getString(cardData, "id");
                if (certificationIdHex == null || certificationIdHex.isEmpty()) {
                    continue;
                }

                UUID certificationId = UlidConverter.hexToUuid(certificationIdHex);

                String orderIdHex = getString(cardData, "order_id");
                if (orderIdHex == null || orderIdHex.isEmpty()) {
                    continue;
                }

                Optional<Order> orderOpt = orderRepository.findBySymfonyOrderId(orderIdHex);
                if (orderOpt.isEmpty()) {
                    continue;
                }

                UUID orderId = orderOpt.get().getId();

                String cardName = getString(cardData, "card_name");
                String codeBarre = getString(cardData, "code_barre");
                if (codeBarre == null || codeBarre.isEmpty()) {
                    codeBarre = certificationIdHex;
                }

                Boolean gradingCompleted = getBoolean(cardData, "grading_completed", false);
                Boolean certificationCompleted = getBoolean(cardData, "certification_completed", false);
                Boolean scanningCompleted = getBoolean(cardData, "scanning_completed", false);
                Boolean packagingCompleted = getBoolean(cardData, "packaging_completed", false);

                Object[] args = new Object[] {
                        uuidToBytes(certificationId),
                        uuidToBytes(orderId),
                        cardName,
                        codeBarre,
                        gradingCompleted,
                        certificationCompleted,
                        scanningCompleted,
                        packagingCompleted,
                        now,
                        0,
                        "FR",
                        now,
                        now
                };

                batchArgs.add(args);

            } catch (Exception e) {
                log.warn(" Error preparing card: {}", e.getMessage());
            }
        }

        int successCount = 0;
        try {
            int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
            for (int result : results) {
                if (result > 0 || result == -2) {
                    successCount++;
                }
            }

            log.debug(" Batch {} completed: {}/{} cards", batchNumber, successCount, batchData.size());

        } catch (Exception e) {
            log.error(" Batch {} failed: {}", batchNumber, e.getMessage());
        }

        return successCount;
    }

    /**
     * Sync a batch of cards (called by MinimalSyncController)
     */
    public int syncCardsBatch(List<Map<String, Object>> cardsData, int batchSize) {
        if (cardsData == null || cardsData.isEmpty()) {
            return 0;
        }
        return insertBatchJdbcLegacy(cardsData, 0);
    }

    /**
     * Get total number of cards in database
     */
    public long getTotalCards() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM card_certification",
                Long.class
        );
        return count != null ? count : 0;
    }

    /**
     * Get sync statistics
     */
    public Map<String, Object> getSyncStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            String sql = "SELECT " +
                    "COUNT(*) as total, " +
                    "SUM(CASE WHEN grading_completed = 0 OR grading_completed IS NULL THEN 1 ELSE 0 END) as needs_grading, " +
                    "SUM(CASE WHEN certification_completed = 0 OR certification_completed IS NULL THEN 1 ELSE 0 END) as needs_cert, " +
                    "SUM(CASE WHEN scanning_completed = 0 OR scanning_completed IS NULL THEN 1 ELSE 0 END) as needs_scan, " +
                    "SUM(CASE WHEN packaging_completed = 0 OR packaging_completed IS NULL THEN 1 ELSE 0 END) as needs_pack " +
                    "FROM card_certification";

            Map<String, Object> result = jdbcTemplate.queryForMap(sql);

            long total = ((Number) result.get("total")).longValue();
            long needsGrading = result.get("needs_grading") != null ?
                    ((Number) result.get("needs_grading")).longValue() : 0;
            long needsCert = result.get("needs_cert") != null ?
                    ((Number) result.get("needs_cert")).longValue() : 0;
            long needsScan = result.get("needs_scan") != null ?
                    ((Number) result.get("needs_scan")).longValue() : 0;
            long needsPack = result.get("needs_pack") != null ?
                    ((Number) result.get("needs_pack")).longValue() : 0;

            stats.put("total_cards", total);
            stats.put("needs_grading", needsGrading);
            stats.put("needs_certification", needsCert);
            stats.put("needs_scanning", needsScan);
            stats.put("needs_packaging", needsPack);

            // Calculate completion
            if (total > 0) {
                double avgCompletion = 100.0 - ((needsGrading + needsCert + needsScan + needsPack) * 25.0 / total);
                stats.put("completion_percentage", Math.round(avgCompletion * 100.0) / 100.0);
            } else {
                stats.put("completion_percentage", 0.0);
            }

        } catch (Exception e) {
            log.error(" Error getting sync stats", e);
            stats.put("error", e.getMessage());
        }

        return stats;
    }

    // ==================== STATUS DETERMINATION METHODS ====================

    /**
     * Determine if grading is completed based on status and grade
     * Card statuses may differ from order statuses
     */
    private boolean determineGradingCompleted(Integer status, String grade) {
        // If grade is set, grading is completed
        if (grade != null && !grade.isEmpty()) {
            return true;
        }
        // Status 3+ typically means past grading stage
        return status != null && status >= 3;
    }

    /**
     * Determine if certification is completed based on status
     */
    private boolean determineCertificationCompleted(Integer status) {
        // Status 4+ typically means past certification stage
        return status != null && status >= 4;
    }

    /**
     * Determine if scanning is completed based on status
     */
    private boolean determineScanningCompleted(Integer status) {
        // Status 5+ or specific scanning complete status
        return status != null && status >= 5;
    }

    /**
     * Determine if packaging is completed based on status
     */
    private boolean determinePackagingCompleted(Integer status) {
        // Status indicates shipped or delivered
        return status != null && (status == 5 || status == 42);
    }

    // ==================== HELPER METHODS ====================

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString().trim() : null;
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float getFloat(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        if (value instanceof String) {
            String str = ((String) value).toLowerCase();
            return str.equals("true") || str.equals("1") || str.equals("yes");
        }

        return defaultValue;
    }

    /**
     * Convert UUID to BINARY(16) bytes
     */
    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        byte[] bytes = new byte[16];
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            bytes[i] = (byte) (lsb >>> (8 * (7 - (i - 8))));
        }
        return bytes;
    }
}
