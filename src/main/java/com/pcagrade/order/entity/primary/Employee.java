package com.pcagrade.order.entity.primary;

import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Employee Entity - English Version
 * Represents an employee in the Pokemon card processing system
 */
@Entity
@Table(name = "j_employee")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends AbstractUlidEntity {

    /**
     * Employee's first name
     */
    @Column(name = "first_name", nullable = false, length = 100)
    @NotBlank(message = "First name is required")
    private String firstName;

    /**
     * Employee's last name
     */
    @Column(name = "last_name", nullable = false, length = 100)
    @NotBlank(message = "Last name is required")
    private String lastName;

    /**
     * Employee's email address
     */
    @Column(name = "email", length = 150)
    @Email(message = "Email should be valid")
    private String email;

    /**
     * Work hours per day (default: 8 hours)
     */
    @Column(name = "work_hours_per_day")
    @Positive(message = "Work hours per day must be positive")
    @Builder.Default
    private Integer workHoursPerDay = 8;

    /**
     * Whether the employee is active
     */
    @Column(name = "active")
    @NotNull
    @Builder.Default
    private Boolean active = true;

    /**
     * Efficiency rating (default: 1.0 = 100%)
     */
    @Column(name = "efficiency_rating")
    @Builder.Default
    private Double efficiencyRating = 1.0;

    /**
     * Date when the employee was created
     */
    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    /**
     * Date when the employee was last modified
     */
    @Column(name = "modification_date")
    private LocalDateTime modificationDate;

    // ========== RELATIONSHIPS ==========

    /**
     * Planning assignments for this employee
     */
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Planning> plannings = new ArrayList<>();

    // ========== BUSINESS METHODS ==========

    /**
     * Get employee's full name
     * @return firstName + " " + lastName
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get employee's initials
     * @return first letter of firstName + first letter of lastName
     */
    public String getInitials() {
        if (firstName != null && lastName != null &&
                !firstName.isEmpty() && !lastName.isEmpty()) {
            return (firstName.charAt(0) + "" + lastName.charAt(0)).toUpperCase();
        }
        return "??";
    }

    /**
     * Check if employee is available for planning
     * @return true if active and has valid work hours
     */
    public boolean isAvailableForPlanning() {
        return Boolean.TRUE.equals(active) &&
                workHoursPerDay != null &&
                workHoursPerDay > 0;
    }

    /**
     * Get effective work minutes per day
     * @return work hours converted to minutes
     */
    public int getWorkMinutesPerDay() {
        return workHoursPerDay != null ? workHoursPerDay * 60 : 480; // 8 hours default
    }

    /**
     * Get adjusted work capacity based on efficiency
     * @return effective minutes considering efficiency rating
     */
    public int getEffectiveWorkMinutesPerDay() {
        double efficiency = efficiencyRating != null ? efficiencyRating : 1.0;
        return (int) (getWorkMinutesPerDay() * efficiency);
    }

    /**
     * Get display name for UI
     * @return formatted name for display
     */
    public String getDisplayName() {
        return getFullName() + (Boolean.FALSE.equals(active) ? " (Inactive)" : "");
    }

    // ========== LIFECYCLE METHODS ==========

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.creationDate == null) {
            this.creationDate = now;
        }
        if (this.modificationDate == null) {
            this.modificationDate = now;
        }

        // Set defaults
        if (this.active == null) {
            this.active = true;
        }
        if (this.workHoursPerDay == null) {
            this.workHoursPerDay = 8;
        }
        if (this.efficiencyRating == null) {
            this.efficiencyRating = 1.0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.modificationDate = LocalDateTime.now();
    }
}