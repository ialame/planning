package com.pcagrade.order.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * - A_PREPARER (status=4) → ROLE_PREPARATEUR
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

    // Configurable break time between tasks
    @Value("${planning.task.break.minutes:5}")
    private int taskBreakMinutes;

    // Order Status Constants
    private static final int STATUS_A_NOTER = 2;      // To be graded
    private static final int STATUS_A_CERTIFIER = 3;  // To be certified/encapsulated
    private static final int STATUS_A_PREPARER = 4;   // To be prepared

    // Planning Status Constants (use order status as planning status)
    private static final int PLANNING_STATUS_A_NOTER = 2;
    private static final int PLANNING_STATUS_A_CERTIFIER = 3;
    private static final int PLANNING_STATUS_A_PREPARER = 4;

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
                cleanExistingPlanning(planningDate);
            }

            // Step 2: Get orders by status
            log.info("📋 Fetching orders A_NOTER (status=2)...");
            List<Map<String, Object>> ordersToGrade = getOrdersByStatus(STATUS_A_NOTER);

            log.info("📋 Fetching orders A_CERTIFIER (status=3)...");
            List<Map<String, Object>> ordersToCertify = getOrdersByStatus(STATUS_A_CERTIFIER);

            log.info("📋 Fetching orders A_PREPARER (status=4)...");
            List<Map<String, Object>> ordersToPrepare = getOrdersByStatus(STATUS_A_PREPARER);

            log.info("📋 Found {} orders to grade, {} orders to certify, {} orders to prepare",
                    ordersToGrade.size(), ordersToCertify.size(), ordersToPrepare.size());

            // Step 3: Get employees by role
            log.info("👥 Fetching ROLE_NOTEUR employees...");
            List<Map<String, Object>> graders = getEmployeesByRole("ROLE_NOTEUR");

            log.info("👥 Fetching ROLE_CERTIFICATEUR employees...");
            List<Map<String, Object>> certifiers = getEmployeesByRole("ROLE_CERTIFICATEUR");

            log.info("👥 Fetching ROLE_PREPARATEUR employees...");
            List<Map<String, Object>> preparers = getEmployeesByRole("ROLE_PREPARATEUR");

            log.info("👥 Found {} graders, {} certifiers, {} preparers",
                    graders.size(), certifiers.size(), preparers.size());

            // Validate we have employees
            if (graders.isEmpty() && !ordersToGrade.isEmpty()) {
                result.put("success", false);
                result.put("message", "No NOTEUR employees found but there are orders to grade");
                return result;
            }

            if (certifiers.isEmpty() && !ordersToCertify.isEmpty()) {
                result.put("success", false);
                result.put("message", "No CERTIFICATEUR employees found but there are orders to certify");
                return result;
            }

            if (preparers.isEmpty() && !ordersToPrepare.isEmpty()) {
                result.put("success", false);
                result.put("message", "No PREPARATEUR employees found but there are orders to prepare");
                return result;
            }

            // Step 4: Track workloads
            Map<String, EmployeeWorkload> graderWorkloads = new HashMap<>();
            graders.forEach(e -> graderWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            Map<String, EmployeeWorkload> certifierWorkloads = new HashMap<>();
            certifiers.forEach(e -> certifierWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            Map<String, EmployeeWorkload> preparerWorkloads = new HashMap<>();
            preparers.forEach(e -> preparerWorkloads.put((String) e.get("id"), new EmployeeWorkload(e)));

            // Step 5: Assign orders with specific status for each type
            int plannedGrading = assignOrdersToEmployees(ordersToGrade, graderWorkloads, planningDate, STATUS_A_NOTER);
            int plannedCertification = assignOrdersToEmployees(ordersToCertify, certifierWorkloads, planningDate, STATUS_A_CERTIFIER);
            int plannedPreparation = assignOrdersToEmployees(ordersToPrepare, preparerWorkloads, planningDate, STATUS_A_PREPARER);

            // Step 6: Build result
            result.put("success", true);
            result.put("message", String.format("✅ Planning completed: %d grading + %d certification + %d preparation tasks",
                    plannedGrading, plannedCertification, plannedPreparation));
            result.put("totalPlanned", plannedGrading + plannedCertification + plannedPreparation);
            result.put("plannedGrading", plannedGrading);
            result.put("plannedCertification", plannedCertification);
            result.put("plannedPreparation", plannedPreparation);
            result.put("startDate", planningDate.toString());
            result.put("graderWorkloads", buildWorkloadSummary(graderWorkloads));
            result.put("certifierWorkloads", buildWorkloadSummary(certifierWorkloads));
            result.put("preparerWorkloads", buildWorkloadSummary(preparerWorkloads));

            log.info("🎉 Planning completed successfully: {} total tasks scheduled across multiple days starting from {}",
                    plannedGrading + plannedCertification + plannedPreparation, planningDate);
            return result;

        } catch (Exception e) {
            log.error("❌ Error executing role-based planning: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }

    /**
     * 🧹 Clean existing planning for a specific date
     */
    private void cleanExistingPlanning(LocalDate planningDate) {
        try {
            String deleteSql = "DELETE FROM j_planning WHERE planning_date = ?";
            Query query = entityManager.createNativeQuery(deleteSql);
            query.setParameter(1, planningDate);
            int deleted = query.executeUpdate();
            log.info("🧹 Cleaned {} existing planning entries for date: {}", deleted, planningDate);
        } catch (Exception e) {
            log.error("❌ Error cleaning planning: {}", e.getMessage());
        }
    }

    /**
     * 📋 Get orders by status (NO date filter)
     * ✅ FIXED: Using real card count from card_certification_order
     */
    private List<Map<String, Object>> getOrdersByStatus(int status) {
        try {
            // ✅ FIXED: Added alias 'o' to table order for subquery
            String sql = """
                SELECT 
                    HEX(o.id) as id,
                    o.num_commande as orderNumber,
                    COALESCE(
                        (SELECT COUNT(*) 
                         FROM card_certification_order cco 
                         WHERE cco.order_id = o.id), 
                        0
                    ) as cardCount,
                    o.delai,
                    o.date as orderDate
                FROM `order` o
                WHERE o.status = ?
                  AND o.annulee = 0
                  AND o.paused = 0
                ORDER BY 
                    CASE o.delai 
                        WHEN 'X' THEN 5
                        WHEN 'F+' THEN 4
                        WHEN 'F' THEN 3
                        WHEN 'C' THEN 2
                        WHEN 'E' THEN 1
                        ELSE 0
                    END DESC,
                    o.date ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, status);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> orders = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> order = new HashMap<>();
                order.put("id", (String) row[0]);
                order.put("orderNumber", (String) row[1]);
                order.put("cardCount", ((Number) row[2]).intValue());
                order.put("delai", (String) row[3]);
                order.put("orderDate", row[4]);
                orders.add(order);
            }

            return orders;

        } catch (Exception e) {
            log.error("❌ Error fetching orders by status: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 👥 Get employees by role (using j_employee_group and j_group)
     * ✅ FIXED: Using correct table names j_employee_group and j_group
     */
    private List<Map<String, Object>> getEmployeesByRole(String roleName) {
        try {
            String sql = """
                SELECT DISTINCT
                    HEX(e.id) as id,
                    e.first_name as firstName,
                    e.last_name as lastName,
                    e.work_hours_per_day as workHoursPerDay,
                    g.name as role
                FROM j_employee e
                INNER JOIN j_employee_group eg ON e.id = eg.employee_id
                INNER JOIN j_group g ON eg.group_id = g.id
                WHERE g.name = ? 
                  AND e.active = 1
                  AND g.active = 1
                ORDER BY e.first_name, e.last_name
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, roleName);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> employee = new HashMap<>();
                employee.put("id", (String) row[0]);
                employee.put("firstName", (String) row[1]);
                employee.put("lastName", (String) row[2]);
                employee.put("workHoursPerDay", ((Number) row[3]).intValue());
                employee.put("role", (String) row[4]);
                employees.add(employee);
            }

            return employees;

        } catch (Exception e) {
            log.error("❌ Error fetching employees by role: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 🎯 Assign orders to employees using least-loaded strategy
     *
     * @param orders List of orders to assign
     * @param workloads Map of employee workloads
     * @param planningDate Initial planning date
     * @param planningStatus Status to set in j_planning (2=A_NOTER, 3=A_CERTIFIER, 4=A_PREPARER)
     * @return Number of orders successfully planned
     */
    private int assignOrdersToEmployees(
            List<Map<String, Object>> orders,
            Map<String, EmployeeWorkload> workloads,
            LocalDate planningDate,
            int planningStatus) {

        int plannedCount = 0;

        for (Map<String, Object> order : orders) {
            // Find least loaded employee
            EmployeeWorkload leastLoaded = workloads.values().stream()
                    .min(Comparator.comparingInt(EmployeeWorkload::getCurrentWorkloadMinutes))
                    .orElse(null);

            if (leastLoaded == null) {
                log.warn("⚠️ No employees available for order: {}", order.get("orderNumber"));
                continue;
            }

            // Calculate task details
            int cardCount = (Integer) order.get("cardCount");
            int durationMinutes = cardCount * TIME_PER_CARD_MINUTES;

            // Calculate start time with improved logic (tasks chain across days)
            LocalDateTime startTime = calculateStartTime(leastLoaded, planningDate, durationMinutes);
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            // Get the actual planning date (may be different from initial planningDate)
            LocalDate actualPlanningDate = startTime.toLocalDate();

            // Save planning with actual date (may span multiple days)
            String planningId = UUID.randomUUID().toString().replace("-", "");
            boolean saved = savePlanning(
                    planningId,
                    (String) order.get("id"),
                    leastLoaded.getEmployeeId(),
                    actualPlanningDate,
                    startTime,
                    endTime,
                    durationMinutes,
                    (String) order.get("delai"),
                    cardCount,
                    planningStatus
            );

            if (saved) {
                leastLoaded.addWorkload(durationMinutes, endTime);
                plannedCount++;
                log.info("✅ Planned order {} for employee {} on {} (status={}, workload: {}min, tasks span {} days)",
                        order.get("orderNumber"),
                        leastLoaded.getEmployeeName(),
                        actualPlanningDate,
                        planningStatus,
                        leastLoaded.getCurrentWorkloadMinutes(),
                        java.time.temporal.ChronoUnit.DAYS.between(planningDate, actualPlanningDate) + 1);
            }
        }

        return plannedCount;
    }

    /**
     * ⏰ Calculate optimal start time for a task
     * ✅ IMPROVED: Tasks chain from 9am to 5pm, then continue next day
     * Uses configurable break time between tasks
     */
    private LocalDateTime calculateStartTime(EmployeeWorkload workload, LocalDate planningDate, int taskDurationMinutes) {
        // If employee has no previous tasks, start at 9am on planning date
        if (workload.getLastEndTime() == null) {
            return planningDate.atTime(WORK_START_TIME);
        }

        // Calculate when this task would start (after last task + configurable break)
        LocalDateTime potentialStartTime = workload.getLastEndTime().plusMinutes(taskBreakMinutes);

        // Calculate when this task would end
        LocalDateTime potentialEndTime = potentialStartTime.plusMinutes(taskDurationMinutes);

        // Check if the task would fit in the same working day
        if (potentialStartTime.toLocalTime().isBefore(WORK_END_TIME) &&
                potentialEndTime.toLocalTime().isBefore(WORK_END_TIME.plusMinutes(1))) {
            // Task fits in the same day
            return potentialStartTime;
        }

        // Task doesn't fit - move to next day at 9am
        LocalDate nextDay = potentialStartTime.toLocalDate().plusDays(1);
        return nextDay.atTime(WORK_START_TIME);
    }

    /**
     * 💾 Save planning to database
     *
     * ✅ FIXED: Using INT for status instead of String 'SCHEDULED'
     *
     * @param planningId Unique planning ID
     * @param orderId Order ID
     * @param employeeId Employee ID
     * @param planningDate Date of the planning
     * @param startTime Start time
     * @param endTime End time
     * @param durationMinutes Duration in minutes
     * @param delai Priority code (X, F+, F, C, E)
     * @param cardCount Number of cards
     * @param status Planning status (2=A_NOTER, 3=A_CERTIFIER, 4=A_PREPARER)
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
            int cardCount,
            int status) {

        try {
            // ✅ Use provided status (2, 3, or 4)
            String sql = """
                INSERT INTO j_planning 
                (id, order_id, employee_id, planning_date, start_time, end_time,
                 estimated_duration_minutes, status, 
                 completed, card_count, delai, created_at, updated_at)
                VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?,
                        ?, ?, 0, ?, ?, NOW(), NOW())
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planningId);
            query.setParameter(2, orderId.replace("-", ""));
            query.setParameter(3, employeeId.replace("-", ""));
            query.setParameter(4, planningDate);
            query.setParameter(5, startTime);
            query.setParameter(6, endTime);
            query.setParameter(7, durationMinutes);
            query.setParameter(8, status);
            query.setParameter(9, cardCount);
            query.setParameter(10, delai);

            int result = query.executeUpdate();

            if (result > 0) {
                log.debug("✅ Planning saved: order={}, employee={}, status={}",
                        orderId, employeeId, status);
            }

            return result > 0;

        } catch (Exception e) {
            log.error("❌ Error saving planning: {}", e.getMessage(), e);
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
            return maxMinutesPerDay > 0 ?
                    (int) ((currentWorkloadMinutes * 100.0) / maxMinutesPerDay) : 0;
        }

        public String getStatus() {
            int percentage = getWorkloadPercentage();
            if (percentage >= 100) return "FULL";
            if (percentage >= 75) return "HIGH";
            if (percentage >= 50) return "MEDIUM";
            return "LOW";
        }
    }
}