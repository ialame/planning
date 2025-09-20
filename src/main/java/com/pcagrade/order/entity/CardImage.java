package com.pcagrade.order.entity;

import com.pcagrade.order.util.AbstractUlidEntity;
import com.pcagrade.order.util.LocalizationColumnDefinitions;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * CardImage entity - SHARED TABLE WARNING
 * This table is shared with other projects, handle with care
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "card_image")
public class CardImage extends AbstractUlidEntity {

    /**
     * FIXED: Use String instead of UUID to match existing database schema
     * This prevents foreign key constraint issues during migration
     */
    @Column(name = "card_id", nullable = false, length = 255)
    private UUID cardId;

    /**
     * Language/localization for this card image
     */
    @Column(name = "langue", nullable = false, columnDefinition = LocalizationColumnDefinitions.DEFINITION)
    private Localization localization;

    /**
     * Reference to the actual image file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "image_id")
    private Image image;

    /**
     * File path or reference
     */
    @Column(name = "fichier", nullable = false)
    private String fichier = "";

    /**
     * Image traits and characteristics stored as JSON
     */
    @Column(name = "traits", nullable = false, columnDefinition = "longtext")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> traits = Collections.emptyMap();

    /**
     * Status code for the image
     */
    @Column(name = "statut", nullable = false)
    private Integer statut = 0;

    /**
     * Additional information stored as JSON
     */
    @Column(name = "infos", nullable = false, columnDefinition = "longtext")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> infos = Collections.emptyMap();

    /**
     * When the image was downloaded
     */
    @Column(name = "downloaded_at", nullable = false)
    private Instant downloadedAt = Instant.now();

    /**
     * Image size information
     */
    @Column(name = "taille_img", length = 50)
    private String tailleImg;

    /**
     * Cards reference
     */
    @Column(name = "cards")
    private String cards;

    /**
     * Source reference
     */
    @Column(name = "src")
    private String src;

    // ========== BUSINESS METHODS ==========

    /**
     * Check if this image is available for planning
     */
    public boolean isAvailableForPlanning() {
        return statut != null && statut == 1; // Assuming 1 = available
    }

    /**
     * Get display name for planning purposes
     */
    public String getDisplayName() {
        if (fichier != null && !fichier.isEmpty()) {
            return fichier;
        }
        return "Image " + getUlidString();
    }
}