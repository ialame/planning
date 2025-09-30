package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 🎯 IMPROVED PLANNING SERVICE - Role-Based Assignment
 *
 * Features:
 * - Assigns orders based on status and employee roles
 * - A_NOTER (status=2) → ROLE_NOTEUR
 * - A_CERTIFIER (status=3) → ROLE_CERTIFICATEUR
 * - No start date filter - processes ALL pending orders
 * - Respects employee workload and working hours
 */
@Service
@Transactional
public class ImprovedPlanningService {

    private static final Logger log = LoggerFactory.getLogger(ImprovedPlanningService.class);

    @Autowired
    private EntityManager entityManager;

    // Constants
    private static final int TIME_PER_CARD_MINUTES = 3;
    private static final LocalTime WORK_START_TIME = LocalTime.of(9, 0);
    private static final LocalTime WORK_END_TIME = LocalTime.of(18, 0);

    // Order Status Constants
    private static final int STATUS_A_NOTER = 2;      // To be evaluated
    private static final int STATUS_A_CERTIFIER = 3;  // To be encapsulated

    /**
     * 🚀 MAIN PLANNING EXECUTION METHOD
     *
     * @param planningDate The date to schedule the work
     * @param cleanFirst Whether to clean existing planning for this date
     * @return Planning result with statistics
     */
    public Map<String, Object> executeRoleBasedPlanning(LocalDate planningDate, boolean cleanFirst) {
        Map<String, Object> result = new HashMap<>();

        try {
            log.info("🎯 Starting Role-Based Planning for date: {}", planningDate);

            // Step 1: Clean existing planning if requested
            if (cleanFirst) {
                cleanExistingPlannings(planningDate);
            }

            // Step 2: Get orders by status
            log.info("📋 Fetching orders A_NOTER (status=2)...");
            List<Map<String, Object>> ordersToNote = getOrdersByStatus(STATUS_A_NOTER);

            log.info("📋 Fetching orders A_CERTIFIER (status=3)...");
            List<Map<String, Object>> ordersToCertify = getOrdersByStatus(STATUS_A_CERTIFIER);

            log.info("📋 Found {} orders A_NOTER and {} orders A_CERTIFIER",
                    ordersToNote.size(), ordersToCertify.size());

            // Step 3: Get employees by role
            log.info("👥 Fetching ROLE_NOTEUR employees...");
            List<Map<String, Object>> noteurs = getEmployeesByRole("ROLE_NOTEUR");

            log.info("👥 Fetching ROLE_CERTIFICATEUR employees...");
            List<Map<String, Object>> certificateurs = getEmployeesByRole("ROLE_CERTIFICATEUR");

            log.info("👥 Found {} NOTEUR and {} CERTIFICATEUR",
                    noteurs.size(), certificateurs.size());

            // Validate we have employees
            if (noteurs.isEmpty() && !ordersToNote.isEmpty()) {
                result.put("success", false);
                result.put("message", "No NOTEUR employees found but there are orders to note");
                return result;
            }

            if (certificateurs.isEmpty() && !ordersToCertify.isEmpty()) {
                result.put("success", false);
                result.put("message", "No CERTIFICATEUR employees found but there are orders to certify");
                return result;
            }

            // Step 4: Initialize workload tracking
            Map<String, EmployeeWorkload> noteurWorkloads = initializeWorkloads(noteurs);
            Map<String, EmployeeWorkload> certificateurWorkloads = initializeWorkloads(certificateurs);

            // Step 5: Assign orders to employees
            List<Map<String, Object>> createdPlannings = new ArrayList<>();
            int successCount = 0;

            // Assign A_NOTER orders to NOTEUR
            successCount += assignOrdersToEmployees(
                    ordersToNote,
                    noteurWorkloads,
                    planningDate,
                    "NOTEUR",
                    createdPlannings
            );

            // Assign A_CERTIFIER orders to CERTIFICATEUR
            successCount += assignOrdersToEmployees(
                    ordersToCertify,
                    certificateurWorkloads,
                    planningDate,
                    "CERTIFICATEUR",
                    createdPlannings
            );

            // Step 6: Calculate statistics
            int totalCards = createdPlannings.stream()
                    .mapToInt(p -> (Integer) p.getOrDefault("cardCount", 0))
                    .sum();

            int totalMinutes = createdPlannings.stream()
                    .mapToInt(p -> (Integer) p.getOrDefault("durationMinutes", 0))
                    .sum();

            // Build result
            result.put("success", true);
            result.put("message", String.format("✅ Planning completed: %d orders assigned", successCount));
            result.put("planningDate", planningDate.toString());
            result.put("totalOrdersAssigned", successCount);
            result.put("ordersToNote", ordersToNote.size());
            result.put("ordersToCertify", ordersToCertify.size());
            result.put("noteursUsed", noteurs.size());
            result.put("certificateursUsed", certificateurs.size());
            result.put("totalCards", totalCards);
            result.put("totalMinutes", totalMinutes);
            result.put("totalHours", String.format("%.1f", totalMinutes / 60.0));
            result.put("createdPlannings", createdPlannings);
            result.put("algorithm", "ROLE_BASED_GREEDY");

            // Add workload summary
            result.put("noteurWorkloads", buildWorkloadSummary(noteurWorkloads));
            result.put("certificateurWorkloads", buildWorkloadSummary(certificateurWorkloads));

            log.info("🎉 Planning completed successfully: {} orders, {} cards, {}h",
                    successCount, totalCards, String.format("%.1f", totalMinutes / 60.0));

            return result;

        } catch (Exception e) {
            log.error("❌ Error in role-based planning", e);
            result.put("success", false);
            result.put("message", "Planning failed: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            return result;
        }
    }

    /**
     * 🗑️ Clean existing plannings for a specific date
     */
    private void cleanExistingPlannings(LocalDate planningDate) {
        try {
            String deleteSql = "DELETE FROM j_planning WHERE planning_date = ?";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            deleteQuery.setParameter(1, planningDate);
            int deleted = deleteQuery.executeUpdate();
            log.info("🗑️ Cleaned {} existing plannings for date {}", deleted, planningDate);
        } catch (Exception e) {
            log.warn("⚠️ Error cleaning plannings: {}", e.getMessage());
        }
    }

    /**
     * 📋 Get orders by status
     */
    private List<Map<String, Object>> getOrdersByStatus(int status) {
        String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as orderNumber,
                o.delai as priority,
                CAST(o.status AS SIGNED) as status,
                COALESCE(
                    (SELECT COUNT(*) FROM card_certification_order cco WHERE cco.order_id = o.id),
                    10
                ) as cardCount
            FROM `order` o
            WHERE o.status = ?
              AND o.annulee = 0
              AND o.paused = 0
            ORDER BY 
                CASE o.delai 
                    WHEN 'X' THEN 1
                    WHEN 'F+' THEN 2
                    WHEN 'F' THEN 3
                    WHEN 'C' THEN 4
                    WHEN 'E' THEN 5
                    ELSE 6
                END ASC,
                o.date ASC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, status);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> {
            Map<String, Object> order = new HashMap<>();
            order.put("id", row[0]);
            order.put("orderNumber", row[1]);
            order.put("delai", row[2]);  // Changed from priority to delai

            // Safe cast for status
            Object statusValue = row[3];
            int statusInt;
            if (statusValue instanceof Number) {
                statusInt = ((Number) statusValue).intValue();
            } else {
                statusInt = status; // fallback to parameter
            }
            order.put("status", statusInt);

            // Safe cast for cardCount
            Object cardCountValue = row[4];
            int cardCount;
            if (cardCountValue instanceof Number) {
                cardCount = ((Number) cardCountValue).intValue();
            } else {
                cardCount = 10; // fallback
            }
            order.put("cardCount", cardCount);

            return order;
        }).collect(Collectors.toList());
    }

    /**
     * 👥 Get employees by role using j_employee_group junction table
     */
    private List<Map<String, Object>> getEmployeesByRole(String roleName) {
        String sql = """
            SELECT DISTINCT
                HEX(e.id) as id,
                e.first_name as firstName,
                e.last_name as lastName,
                e.email,
                e.work_hours_per_day as workHoursPerDay,
                e.active
            FROM j_employee e
            INNER JOIN j_employee_group eg ON e.id = eg.employee_id
            INNER JOIN j_group g ON eg.group_id = g.id
            WHERE g.name = ?
              AND e.active = 1
              AND g.active = 1
            ORDER BY e.last_name, e.first_name
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter(1, roleName);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        return results.stream().map(row -> {
            Map<String, Object> employee = new HashMap<>();
            employee.put("id", row[0]);
            employee.put("firstName", row[1]);
            employee.put("lastName", row[2]);
            employee.put("email", row[3]);
            employee.put("workHoursPerDay", row[4] != null ? ((Number) row[4]).intValue() : 8);

            // Handle active column - can be Boolean or Number depending on database
            Object activeValue = row[5];
            boolean isActive;
            if (activeValue instanceof Boolean) {
                isActive = (Boolean) activeValue;
            } else if (activeValue instanceof Number) {
                isActive = ((Number) activeValue).intValue() == 1;
            } else {
                isActive = true; // Default to active
            }
            employee.put("active", isActive);
            employee.put("role", roleName);
            return employee;
        }).collect(Collectors.toList());
    }

    /**
     * 🏗️ Initialize workload tracking for employees
     */
    private Map<String, EmployeeWorkload> initializeWorkloads(List<Map<String, Object>> employees) {
        Map<String, EmployeeWorkload> workloads = new HashMap<>();
        for (Map<String, Object> employee : employees) {
            String employeeId = (String) employee.get("id");
            workloads.put(employeeId, new EmployeeWorkload(employee));
        }
        return workloads;
    }

    /**
     * 📌 Assign orders to employees with role-based logic
     */
    private int assignOrdersToEmployees(
            List<Map<String, Object>> orders,
            Map<String, EmployeeWorkload> workloads,
            LocalDate planningDate,
            String roleType,
            List<Map<String, Object>> createdPlannings) {

        int successCount = 0;

        for (Map<String, Object> order : orders) {
            try {
                // Find least busy employee
                EmployeeWorkload leastBusy = findLeastBusyEmployee(workloads);
                if (leastBusy == null) {
                    log.warn("⚠️ No available {} for order {}", roleType, order.get("orderNumber"));
                    continue;
                }

                String orderId = (String) order.get("id");
                String employeeId = leastBusy.getEmployeeId();
                int cardCount = (Integer) order.get("cardCount");
                int durationMinutes = Math.max(15, cardCount * TIME_PER_CARD_MINUTES);

                // Calculate optimal start time
                LocalDateTime startTime = calculateStartTime(leastBusy, planningDate);
                LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

                // Get delai directly from order
                String delai = (String) order.get("delai");

                // Save planning to database
                boolean saved = savePlanning(
                        UUID.randomUUID().toString().replace("-", ""),
                        orderId,
                        employeeId,
                        planningDate,
                        startTime,
                        endTime,
                        durationMinutes,
                        delai,
                        cardCount
                );

                if (saved) {
                    successCount++;

                    // Update workload
                    leastBusy.addWorkload(durationMinutes, endTime);

                    // Add to result
                    Map<String, Object> planning = new HashMap<>();
                    planning.put("orderId", orderId);
                    planning.put("orderNumber", order.get("orderNumber"));
                    planning.put("employeeId", employeeId);
                    planning.put("employeeName", leastBusy.getEmployeeName());
                    planning.put("role", roleType);
                    planning.put("delai", delai);
                    planning.put("cardCount", cardCount);
                    planning.put("durationMinutes", durationMinutes);
                    planning.put("startTime", startTime.toString());
                    planning.put("endTime", endTime.toString());

                    createdPlannings.add(planning);

                    log.info("✅ Assigned {} order {} ({}) to {} ({}min, {} cards)",
                            roleType, order.get("orderNumber"), delai,
                            leastBusy.getEmployeeName(), durationMinutes, cardCount);
                }

            } catch (Exception e) {
                log.error("❌ Error assigning order {}: {}",
                        order.get("orderNumber"), e.getMessage());
            }
        }

        return successCount;
    }

    /**
     * 🔍 Find least busy employee from workload map
     */
    private EmployeeWorkload findLeastBusyEmployee(Map<String, EmployeeWorkload> workloads) {
        return workloads.values().stream()
                .min(Comparator.comparing(EmployeeWorkload::getCurrentWorkloadMinutes))
                .orElse(null);
    }

    /**
     * ⏰ Calculate optimal start time for a task
     */
    private LocalDateTime calculateStartTime(EmployeeWorkload workload, LocalDate planningDate) {
        LocalDateTime baseTime = planningDate.atTime(WORK_START_TIME);

        // If employee already has work scheduled, start after last task
        if (workload.getLastEndTime() != null) {
            LocalDateTime afterLastTask = workload.getLastEndTime().plusMinutes(15); // 15min break

            // If it's on the same day and within working hours, use it
            if (afterLastTask.toLocalDate().equals(planningDate) &&
                    afterLastTask.toLocalTime().isBefore(WORK_END_TIME)) {
                return afterLastTask;
            }
        }

        return baseTime;
    }

    /**
     * 💾 Save planning to database
     */
    private boolean savePlanning(
            String planningId,
            String orderId,
            String employeeId,
            LocalDate planningDate,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int durationMinutes,
            String delai,
            int cardCount) {

        try {
            // Use delai directly instead of priority
            String sql = """
                INSERT INTO j_planning 
                (id, order_id, employee_id, planning_date, start_time, end_time,
                 estimated_duration_minutes, status, 
                 completed, card_count,delai, created_at, updated_at)
                VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?,
                        ?, 'SCHEDULED', 0, ?, ?, NOW(), NOW())
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planningId);
            query.setParameter(2, orderId.replace("-", ""));
            query.setParameter(3, employeeId.replace("-", ""));
            query.setParameter(4, planningDate);
            query.setParameter(5, startTime);
            query.setParameter(6, endTime);
            query.setParameter(7, durationMinutes);
            query.setParameter(8, cardCount);
            query.setParameter(9 ,delai);

            int result = query.executeUpdate();
            return result > 0;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 📊 Build workload summary for response
     */
    private List<Map<String, Object>> buildWorkloadSummary(Map<String, EmployeeWorkload> workloads) {
        return workloads.values().stream().map(w -> {
            Map<String, Object> summary = new HashMap<>();
            summary.put("employeeId", w.getEmployeeId());
            summary.put("employeeName", w.getEmployeeName());
            summary.put("totalMinutes", w.getCurrentWorkloadMinutes());
            summary.put("totalHours", String.format("%.1f", w.getCurrentWorkloadMinutes() / 60.0));
            summary.put("workloadPercentage", w.getWorkloadPercentage());
            summary.put("status", w.getStatus());
            return summary;
        }).collect(Collectors.toList());
    }

    // ========== INNER CLASS: EMPLOYEE WORKLOAD TRACKING ==========

    /**
     * 👤 Track individual employee workload
     */
    private static class EmployeeWorkload {
        private final Map<String, Object> employee;
        private int currentWorkloadMinutes;
        private LocalDateTime lastEndTime;

        public EmployeeWorkload(Map<String, Object> employee) {
            this.employee = employee;
            this.currentWorkloadMinutes = 0;
            this.lastEndTime = null;
        }

        public void addWorkload(int minutes, LocalDateTime endTime) {
            this.currentWorkloadMinutes += minutes;
            this.lastEndTime = endTime;
        }

        public int getCurrentWorkloadMinutes() {
            return currentWorkloadMinutes;
        }

        public LocalDateTime getLastEndTime() {
            return lastEndTime;
        }

        public String getEmployeeId() {
            return (String) employee.get("id");
        }

        public String getEmployeeName() {
            return employee.get("firstName") + " " + employee.get("lastName");
        }

        public int getWorkloadPercentage() {
            int workHoursPerDay = (Integer) employee.getOrDefault("workHoursPerDay", 8);
            int maxMinutesPerDay = workHoursPerDay * 60;
            return maxMinutesPerDay > 0 ? (currentWorkloadMinutes * 100) / maxMinutesPerDay : 0;
        }

        public String getStatus() {
            int percentage = getWorkloadPercentage();
            if (percentage >= 100) return "overloaded";
            if (percentage >= 80) return "busy";
            return "available";
        }
    }
}