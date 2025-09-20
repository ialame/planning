package com.pcagrade.order.service;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.repository.EmployeeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Employee Service - English Version
 * Handles all employee-related business logic
 */
@Service
@Transactional
@Validated
@Slf4j
public class EmployeeService {

    private static final int DEFAULT_WORK_HOURS_PER_DAY = 8;
    private static final int MAX_WORK_HOURS_PER_DAY = 12;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * Create a new employee
     * @param employee the employee to create
     * @return created employee
     */
    public Employee createEmployee(@Valid @NotNull Employee employee) {
        try {
            log.info("Creating new employee: {} {}", employee.getFirstName(), employee.getLastName());

            // Validate business rules
            validateNewEmployee(employee);

            // Set default values if not provided
            if (employee.getWorkHoursPerDay() == null) {
                employee.setWorkHoursPerDay(DEFAULT_WORK_HOURS_PER_DAY);
            }
            if (employee.getActive() == null) {
                employee.setActive(true);
            }
            if (employee.getCreationDate() == null) {
                employee.setCreationDate(LocalDateTime.now());
            }
            if (employee.getModificationDate() == null) {
                employee.setModificationDate(LocalDateTime.now());
            }

            Employee savedEmployee = employeeRepository.save(employee);
            log.info("Employee created successfully with ID: {}", savedEmployee.getUlidString());
            return savedEmployee;

        } catch (Exception e) {
            log.error("Error creating employee", e);
            throw new RuntimeException("Error creating employee: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing employee
     * @param employee the employee to update
     * @return updated employee
     */
    public Employee updateEmployee(@Valid @NotNull Employee employee) {
        log.info("Updating employee: {}", employee.getUlidString());

        if (employee.getId() == null) {
            throw new IllegalArgumentException("Employee ID cannot be null for update");
        }

        employee.setModificationDate(LocalDateTime.now());

        Employee updatedEmployee = employeeRepository.save(employee);
        log.info("Employee updated successfully: {}", updatedEmployee.getUlidString());
        return updatedEmployee;
    }

    /**
     * Get employee by ID
     * @param id employee ID
     * @return employee if found
     */
    public Optional<Employee> findById(String id) {
        try {
            log.debug("Finding employee by ID: {}", id);
            UUID uuid = UUID.fromString(id.length() == 32 ?
                    id.replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5") : id);
            return employeeRepository.findById(uuid);
        } catch (Exception e) {
            log.error("Error finding employee by ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Delete an employee
     * @param id the employee ID
     */
    public void deleteEmployee(@NotNull String id) {
        try {
            log.info("Deleting employee: {}", id);

            Optional<Employee> employee = findById(id);
            if (employee.isPresent()) {
                employeeRepository.delete(employee.get());
                log.info("Employee deleted successfully: {}", id);
            } else {
                throw new IllegalArgumentException("Employee not found with ID: " + id);
            }

        } catch (Exception e) {
            log.error("Error deleting employee: {}", e.getMessage());
            throw new RuntimeException("Error deleting employee: " + e.getMessage(), e);
        }
    }

    // ========== BUSINESS LOGIC METHODS ==========

    /**
     * Get all active employees
     */
    /**
     * Get all active employees - FIXED VERSION
     */
    public List<Map<String, Object>> getAllActiveEmployees() {
        try {
            log.debug("Loading active employees from j_employee table...");

            String sql = """
                    SELECT 
                        id as id,  -- Récupérer l'ID binaire directement
                        e.first_name as firstName,
                        e.last_name as lastName,
                        e.email,
                        COALESCE(e.work_hours_per_day, 8) as workHoursPerDay,
                        COALESCE(e.active, 1) as active,
                        COALESCE(e.efficiency_rating, 1.0) as efficiencyRating,
                        e.creation_date as creationDate,
                        e.modification_date as modificationDate
                    FROM j_employee e
                    WHERE COALESCE(e.active, 1) = 1
                    ORDER BY e.last_name, e.first_name
                """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> employees = new ArrayList<>();

            log.info("Found {} active employees in database", results.size());

            for (Object[] row : results) {
                try {
                    Map<String, Object> employee = new HashMap<>();

                    // Convertir l'ID binaire en ULID
                    byte[] idBytes = (byte[]) row[0];
                    ByteBuffer buffer = ByteBuffer.wrap(idBytes);
                    UUID uuid = new UUID(buffer.getLong(), buffer.getLong());
                    Ulid ulid = Ulid.from(uuid);
                    String ulidString = ulid.toString();

                    employee.put("id", ulidString);  // ULID au lieu de HEX
                    employee.put("firstName", firstName);
                    employee.put("lastName", lastName);
                    employee.put("fullName", firstName + " " + lastName);
                    employee.put("email", email);
                    employee.put("workHoursPerDay", workHours);
                    employee.put("active", active);
                    employee.put("efficiencyRating", efficiency);
                    employee.put("creationDate", row[7]);
                    employee.put("modificationDate", row[8]);

                    // Computed fields for frontend
                    employee.put("dailyCapacityMinutes", workHours * 60);
                    employee.put("dailyCardCapacity", (int) Math.floor((workHours * 60) / 3)); // 3 minutes per card

                    employees.add(employee);

                    log.debug("✅ Mapped employee: {} {} (ID: {})", firstName, lastName, idBytes);

                } catch (Exception e) {
                    log.error("❌ Error mapping employee row: {}", e.getMessage());
                    continue; // Skip this employee but continue with others
                }
            }

            log.info("✅ Successfully mapped {} employees for frontend", employees.size());
            return employees;

        } catch (Exception e) {
            log.error("❌ Error loading active employees: {}", e.getMessage(), e);
            return new ArrayList<>(); // Return empty list instead of null
        }
    }
    /**
     * Validate new employee business rules
     */
    private void validateNewEmployee(Employee employee) {
        if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (employee.getLastName() == null || employee.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
            if (!employee.getEmail().contains("@")) {
                throw new IllegalArgumentException("Invalid email format");
            }

            // Check for duplicate email
            if (existsByEmail(employee.getEmail())) {
                throw new IllegalArgumentException("Employee with this email already exists: " + employee.getEmail());
            }
        }

        if (employee.getWorkHoursPerDay() != null &&
                (employee.getWorkHoursPerDay() < 1 || employee.getWorkHoursPerDay() > MAX_WORK_HOURS_PER_DAY)) {
            throw new IllegalArgumentException("Work hours per day must be between 1 and " + MAX_WORK_HOURS_PER_DAY);
        }
    }

    /**
     * Check if employee exists by email
     */
    private boolean existsByEmail(String email) {
        try {
            return employeeRepository.findByEmail(email).isPresent();
        } catch (Exception e) {
            log.warn("Error checking email existence: {}", email, e);
            return false;
        }
    }



}