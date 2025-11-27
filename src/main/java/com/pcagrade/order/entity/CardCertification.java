package com.pcagrade.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CardCertification entity - Using Symfony ID as Primary Key
 *
 * UPDATED: Added new fields from GptOrderController API
 *
 * IMPORTANT: This entity does NOT inherit from AbstractUlidEntity
 * because we use the Symfony certification ID directly as our primary key.
 *
 * This approach:
 * - Eliminates the need for symfony_certification_id as a separate field
 * - Simplifies synchronization (no ID mapping needed)
 * - Reduces database storage (one less column and index)
 * - Ensures perfect consistency between databases
 */
@Data
@Entity
@Table(name = "card_certification")
public class CardCertification implements Serializable {

    private static final long serialVersionUID = 1L;

    // ============================================================
    // PRIMARY KEY - Using Symfony Certification ID directly
    // ============================================================

    /**
     * Primary key: Uses the Symfony certification ID directly
     * We manually set this ID from the Symfony API data during sync
     *
     * CRITICAL: @JdbcTypeCode ensures UUID is stored as BINARY(16) not VARCHAR
     */
    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    // ============================================================
    // ORDER RELATIONSHIP
    // ============================================================

    @Column(name = "order_id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID orderId;

    // ============================================================
    // CARD INFORMATION (From GptOrderController)
    // ============================================================

    /**
     * Card name
     */
    @Column(name = "card_name", length = 255)
    private String cardName;

    /**
     * Barcode / code_barre
     */
    @Column(name = "code_barre", length = 100)
    private String codeBarre;

    /**
     * Card number in the set
     */
    @Column(name = "card_number", length = 50)
    private String cardNumber;

    /**
     * Set/Extension name (e.g., "Base Set", "Jungle")
     */
    @Column(name = "set_name", length = 255)
    private String setName;

    /**
     * Series name
     */
    @Column(name = "serie_name", length = 255)
    private String serieName;

    /**
     * Card language (FR, EN, JP, etc.)
     */
    @Column(name = "langue", length = 10)
    private String langue;

    /**
     * Declared value by customer
     */
    @Column(name = "declared_value")
    private Float declaredValue;

    // ============================================================
    // GRADING INFORMATION (From GptOrderController)
    // ============================================================

    /**
     * Final grade
     */
    @Column(name = "grade", length = 20)
    private String grade;

    /**
     * Grade from first grader
     */
    @Column(name = "grade_1", length = 20)
    private String grade1;

    /**
     * Grade from second grader
     */
    @Column(name = "grade_2", length = 20)
    private String grade2;

    /**
     * Grade from third grader
     */
    @Column(name = "grade_3", length = 20)
    private String grade3;

    // ============================================================
    // CARD ATTRIBUTES (From GptOrderController)
    // ============================================================

    /**
     * Reverse holo indicator
     */
    @Column(name = "reverse", length = 50)
    private String reverse;

    /**
     * Edition (1st Edition, Unlimited, etc.)
     */
    @Column(name = "edition", length = 50)
    private String edition;

    /**
     * Shadowless indicator
     */
    @Column(name = "shadowless", length = 50)
    private String shadowless;

    /**
     * Foil type
     */
    @Column(name = "foil", length = 50)
    private String foil;

    /**
     * CSN (Card Serial Number) if applicable
     */
    @Column(name = "csn", length = 100)
    private String csn;

    /**
     * Multi-grade flag
     */
    @Column(name = "multi_grade")
    private Boolean multiGrade;

    // ============================================================
    // STATUS FLAGS
    // ============================================================

    /**
     * Symfony status code
     */
    @Column(name = "status")
    private Integer status;

    /**
     * Deleted flag
     */
    @Column(name = "is_deleted")
    private Boolean isDeleted;

    // ============================================================
    // PROCESSING COMPLETION FLAGS
    // ============================================================

    /**
     * Whether grading has been completed
     */
    @Column(name = "grading_completed")
    private Boolean gradingCompleted;

    /**
     * Whether certification/encapsulation has been completed
     */
    @Column(name = "certification_completed")
    private Boolean certificationCompleted;

    /**
     * Whether scanning has been completed
     */
    @Column(name = "scanning_completed")
    private Boolean scanningCompleted;

    /**
     * Whether packaging has been completed
     */
    @Column(name = "packaging_completed")
    private Boolean packagingCompleted;

    // ============================================================
    // TIMESTAMPS
    // ============================================================

    /**
     * Date field (original Symfony date)
     */
    @Column(name = "date")
    private LocalDateTime date;

    /**
     * Creation timestamp
     */
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    /**
     * Last modification timestamp
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ============================================================
    // LEGACY FIELDS (For backward compatibility)
    // ============================================================

    /**
     * Legacy card ID reference
     */
    @Column(name = "card_id", columnDefinition = "BINARY(16)")
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID cardId;

    // ============================================================
    // LIFECYCLE CALLBACKS
    // ============================================================

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (creationDate == null) {
            creationDate = now;
        }
        modificationDate = now;

        // Default values
        if (gradingCompleted == null) gradingCompleted = false;
        if (certificationCompleted == null) certificationCompleted = false;
        if (scanningCompleted == null) scanningCompleted = false;
        if (packagingCompleted == null) packagingCompleted = false;
        if (isDeleted == null) isDeleted = false;
        if (multiGrade == null) multiGrade = false;
    }

    @PreUpdate
    protected void onUpdate() {
        modificationDate = LocalDateTime.now();
    }

    // ============================================================
    // BUSINESS LOGIC METHODS
    // ============================================================

    /**
     * Check if all processing stages are complete
     */
    public boolean isFullyProcessed() {
        return Boolean.TRUE.equals(gradingCompleted) &&
                Boolean.TRUE.equals(certificationCompleted) &&
                Boolean.TRUE.equals(scanningCompleted) &&
                Boolean.TRUE.equals(packagingCompleted);
    }

    /**
     * Get current processing stage
     */
    public String getCurrentStage() {
        if (Boolean.TRUE.equals(packagingCompleted)) {
            return "COMPLETED";
        }
        if (Boolean.TRUE.equals(scanningCompleted)) {
            return "PACKAGING";
        }
        if (Boolean.TRUE.equals(certificationCompleted)) {
            return "SCANNING";
        }
        if (Boolean.TRUE.equals(gradingCompleted)) {
            return "CERTIFYING";
        }
        return "GRADING";
    }

    /**
     * Get completion percentage (0-100)
     */
    public int getCompletionPercentage() {
        int completed = 0;
        if (Boolean.TRUE.equals(gradingCompleted)) completed += 25;
        if (Boolean.TRUE.equals(certificationCompleted)) completed += 25;
        if (Boolean.TRUE.equals(scanningCompleted)) completed += 25;
        if (Boolean.TRUE.equals(packagingCompleted)) completed += 25;
        return completed;
    }

    /**
     * Check if card has a grade assigned
     */
    public boolean hasGrade() {
        return grade != null && !grade.isEmpty();
    }

    /**
     * Get display name (card name or barcode)
     */
    public String getDisplayName() {
        if (cardName != null && !cardName.isEmpty()) {
            return cardName;
        }
        if (codeBarre != null && !codeBarre.isEmpty()) {
            return codeBarre;
        }
        return id != null ? id.toString().substring(0, 8) + "..." : "Unknown";
    }
}