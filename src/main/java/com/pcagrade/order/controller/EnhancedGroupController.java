package com.pcagrade.order.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pcagrade.order.dto.GroupDto;
import com.pcagrade.order.entity.Group;
import com.pcagrade.order.service.GroupService;
import com.pcagrade.order.service.mapper.GroupMapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Enhanced Group Controller - Role Management API with DTOs
 * RESTful API for managing groups and roles using DTOs
 */
@RestController
@RequestMapping("/api/v2/groups")
@Tag(name = "Group Management V2", description = "Enhanced API for managing groups and roles with DTOs")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class EnhancedGroupController {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupMapperService groupMapper;

    @Autowired
    private EntityManager entityManager;

    // ========== CRUD OPERATIONS ==========

    /**
     * 📋 GET ALL GROUPS
     * Endpoint: GET /api/v2/groups
     */
    @GetMapping
    @Operation(summary = "Get all active groups", description = "Retrieve all active groups with optional pagination and search")
    @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllGroups(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Include employee count") @RequestParam(defaultValue = "true") boolean includeEmployeeCount) {

        try {
            log.info("🔍 Getting groups - page: {}, size: {}, sortBy: {}, search: '{}'",
                    page, size, sortBy, search);

            Map<String, Object> response = new HashMap<>();

            if (page < 0 || size <= 0) {
                // Return all groups without pagination
                List<Group> groups;
                if (search != null && !search.trim().isEmpty()) {
                    groups = groupService.searchGroups(search.trim());
                } else {
                    groups = groupService.getAllActiveGroups();
                }

                List<GroupDto.GroupInfo> groupDtos = groups.stream()
                        .map(group -> includeEmployeeCount ?
                                groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                groupMapper.toGroupInfo(group))
                        .toList();

                response.put("groups", groupDtos);
                response.put("totalElements", groupDtos.size());
                response.put("totalPages", 1);
                response.put("currentPage", 0);
                response.put("hasNext", false);
                response.put("hasPrevious", false);
            } else {
                // Return paginated results
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                if (search != null && !search.trim().isEmpty()) {
                    // For search, we need to implement pagination manually
                    List<Group> allGroups = groupService.searchGroups(search.trim());
                    int start = Math.min(page * size, allGroups.size());
                    int end = Math.min(start + size, allGroups.size());
                    List<Group> paginatedGroups = allGroups.subList(start, end);

                    List<GroupDto.GroupInfo> groupDtos = paginatedGroups.stream()
                            .map(group -> includeEmployeeCount ?
                                    groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                    groupMapper.toGroupInfo(group))
                            .toList();

                    response.put("groups", groupDtos);
                    response.put("totalElements", allGroups.size());
                    response.put("totalPages", (int) Math.ceil((double) allGroups.size() / size));
                    response.put("currentPage", page);
                    response.put("hasNext", end < allGroups.size());
                    response.put("hasPrevious", page > 0);
                } else {
                    Page<Group> groupPage = groupService.getAllActiveGroups(pageable);

                    List<GroupDto.GroupInfo> groupDtos = groupPage.getContent().stream()
                            .map(group -> includeEmployeeCount ?
                                    groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(group.getId())) :
                                    groupMapper.toGroupInfo(group))
                            .toList();

                    response.put("groups", groupDtos);
                    response.put("totalElements", groupPage.getTotalElements());
                    response.put("totalPages", groupPage.getTotalPages());
                    response.put("currentPage", groupPage.getNumber());
                    response.put("hasNext", groupPage.hasNext());
                    response.put("hasPrevious", groupPage.hasPrevious());
                }
            }

            log.debug("✅ Successfully retrieved {} groups",
                    ((List<?>) response.get("groups")).size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving groups: " + e.getMessage()));
        }
    }

    /**
     * 🔍 GET GROUP BY ID
     * Endpoint: GET /api/v2/groups/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID", description = "Retrieve a specific group by its ID with employee details")
    @ApiResponse(responseCode = "200", description = "Group found")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<GroupDto.GroupWithEmployees> getGroupById(
            @PathVariable String id,
            @Parameter(description = "Include employee details") @RequestParam(defaultValue = "true") boolean includeEmployees) {
        try {
            log.info("🔍 Getting group by ID: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> groupOpt = groupService.findById(groupId);

            if (groupOpt.isPresent()) {
                Group group = groupOpt.get();
                GroupDto.GroupWithEmployees result = includeEmployees ?
                        groupMapper.toGroupWithEmployees(group) :
                        GroupDto.GroupWithEmployees.builder()
                                .groupInfo(groupMapper.toGroupInfo(group, groupService.countActiveEmployeesInGroup(groupId)))
                                .totalEmployees(groupService.countActiveEmployeesInGroup(groupId).intValue())
                                .build();

                log.debug("✅ Group found: {}", group.getName());
                return ResponseEntity.ok(result);
            } else {
                log.warn("⚠️ Group not found: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group ID format: {}", id);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("❌ Error getting group by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * ➕ CREATE NEW GROUP
     * Endpoint: POST /api/v2/groups
     */
    @PostMapping
    @Operation(summary = "Create new group", description = "Create a new group/role using DTO")
    @ApiResponse(responseCode = "201", description = "Group created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid group data")
    public ResponseEntity<Map<String, Object>> createGroup(@Valid @RequestBody GroupDto.CreateGroupRequest request) {
        try {
            log.info("➕ Creating new group: {}", request.getName());

            // Validate and suggest group name format if needed
            if (!groupMapper.isValidGroupName(request.getName())) {
                String suggestedName = groupMapper.suggestGroupNameFormat(request.getName());
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Invalid group name format. Group name must be uppercase with underscores only.",
                                "suggestedName", suggestedName != null ? suggestedName : "EXAMPLE_GROUP_NAME"
                        ));
            }

            Group group = groupMapper.fromCreateRequest(request);
            Group createdGroup = groupService.createGroup(group);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group created successfully");
            response.put("group", groupMapper.toGroupInfo(createdGroup));

            log.info("✅ Group created successfully: {} (ID: {})",
                    createdGroup.getName(), createdGroup.getUlidString());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error creating group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating group: " + e.getMessage()));
        }
    }

    /**
     * ✏️ UPDATE GROUP
     * Endpoint: PUT /api/v2/groups/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update group", description = "Update an existing group using DTO")
    @ApiResponse(responseCode = "200", description = "Group updated successfully")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<Map<String, Object>> updateGroup(
            @PathVariable String id,
            @Valid @RequestBody GroupDto.UpdateGroupRequest request) {
        try {
            log.info("✏️ Updating group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> existingGroupOpt = groupService.findById(groupId);

            if (existingGroupOpt.isEmpty()) {
                log.warn("⚠️ Group not found for update: {}", id);
                return ResponseEntity.notFound().build();
            }

            Group existingGroup = existingGroupOpt.get();
            Group updatedGroup = groupMapper.updateFromRequest(existingGroup, request);
            updatedGroup = groupService.updateGroup(updatedGroup);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group updated successfully");
            response.put("group", groupMapper.toGroupInfo(updatedGroup));

            log.info("✅ Group updated successfully: {} (ID: {})",
                    updatedGroup.getName(), updatedGroup.getUlidString());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error updating group: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating group: " + e.getMessage()));
        }
    }

    /**
     * 🗑️ DELETE GROUP
     * Endpoint: DELETE /api/v2/groups/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group", description = "Deactivate a group (soft delete)")
    @ApiResponse(responseCode = "200", description = "Group deleted successfully")
    @ApiResponse(responseCode = "404", description = "Group not found")
    public ResponseEntity<Map<String, Object>> deleteGroup(@PathVariable String id) {
        try {
            log.info("🗑️ Deleting group: {}", id);

            UUID groupId = UUID.fromString(id);
            Optional<Group> groupOpt = groupService.findById(groupId);

            if (groupOpt.isEmpty()) {
                log.warn("⚠️ Group not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }

            groupService.deleteGroup(groupId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Group deleted successfully");

            log.info("✅ Group deleted successfully: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Invalid group ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error deleting group: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting group: " + e.getMessage()));
        }
    }

    // ========== EMPLOYEE-GROUP MANAGEMENT ==========

// Dans EnhancedGroupController.java
    // SUPPRIMEZ l'ancienne méthode updateEmployeeGroups qui prend EmployeeGroupAssignmentRequest
    // et GARDEZ SEULEMENT celle-ci :

    /**
     * 👥 GET GROUPS FOR EMPLOYEE - MÊME PATTERN QUE EMPLOYEES
     * Endpoint: GET /api/v2/groups/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Map<String, Object>> getGroupsForEmployee(@PathVariable String employeeId) {
        try {
            log.info("👥 Getting groups for employee: {}", employeeId);

            String cleanEmployeeId = employeeId.replace("-", "").toUpperCase();

            String sql = """
            SELECT 
                HEX(g.id) as id,
                g.name,
                g.description,
                g.permission_level as permissionLevel,
                g.active,
                g.creation_date as creationDate,
                g.modification_date as modificationDate
            FROM j_group g
            INNER JOIN j_employee_group eg ON g.id = eg.group_id
            WHERE HEX(eg.employee_id) = ?
            AND g.active = 1
            ORDER BY g.permission_level DESC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, cleanEmployeeId);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> groups = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> group = new HashMap<>();
                group.put("id", (String) row[0]);
                group.put("name", (String) row[1]);
                group.put("description", (String) row[2]);
                group.put("permissionLevel", ((Number) row[3]).intValue());

                // FIX: Gérer le type Boolean correctement
                Object activeValue = row[4];
                boolean isActive;
                if (activeValue instanceof Boolean) {
                    isActive = (Boolean) activeValue;
                } else if (activeValue instanceof Number) {
                    isActive = ((Number) activeValue).intValue() == 1;
                } else {
                    isActive = true; // valeur par défaut
                }
                group.put("active", isActive);

                group.put("creationDate", row[5]);
                group.put("modificationDate", row[6]);
                groups.add(group);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groups);
            response.put("employeeId", employeeId);
            response.put("totalGroups", groups.size());

            log.info("✅ Found {} groups for employee: {}", groups.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting groups for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving groups: " + e.getMessage()));
        }
    }

    /**
     * 🔄 UPDATE EMPLOYEE GROUPS - VERSION AVEC DEBUG ET VALIDATION
     * Endpoint: PUT /api/v2/groups/employee/{employeeId}
     */
    @PutMapping("/employee/{employeeId}")
    @Transactional
    @Operation(summary = "Update employee groups", description = "Replace all groups for an employee")
    public ResponseEntity<Map<String, Object>> updateEmployeeGroups(
            @PathVariable String employeeId,
            @RequestBody Map<String, Object> request) {
        try {
            log.info("Updating groups for employee: {}", employeeId);

            // Convertir tous les IDs (employé et groupes) en UUID
            UUID empUuid = parseId(employeeId);

            @SuppressWarnings("unchecked")
            List<String> groupIdStrings = (List<String>) request.get("groupIds");

            List<UUID> groupUuids = new ArrayList<>();
            if (groupIdStrings != null) {
                for (String groupId : groupIdStrings) {
                    groupUuids.add(parseId(groupId));
                }
            }

            // Supprimer les assignations existantes
            String deleteSql = "DELETE FROM j_employee_group WHERE employee_id = ?";
            Query deleteQuery = entityManager.createNativeQuery(deleteSql);
            deleteQuery.setParameter(1, empUuid);
            int deletedCount = deleteQuery.executeUpdate();

            // Ajouter les nouvelles assignations
            int addedCount = 0;
            for (UUID groupUuid : groupUuids) {
                String insertSql = "INSERT INTO j_employee_group (employee_id, group_id) VALUES (?, ?)";
                Query insertQuery = entityManager.createNativeQuery(insertSql);
                insertQuery.setParameter(1, empUuid);
                insertQuery.setParameter(2, groupUuid);
                insertQuery.executeUpdate();
                addedCount++;
            }

            // Response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee groups updated successfully");
            response.put("assignmentsRemoved", deletedCount);
            response.put("assignmentsAdded", addedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating employee groups: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating groups: " + e.getMessage()));
        }
    }

    /**
     * 🔗 ASSIGN EMPLOYEE TO GROUP
     * Endpoint: POST /api/v2/groups/{groupId}/employees/{employeeId}
     */
    @PostMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Assign employee to group", description = "Add an employee to a group")
    public ResponseEntity<Map<String, Object>> assignEmployeeToGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("🔗 Assigning employee {} to group {}", employeeId, groupId);

            UUID empId = UUID.fromString(employeeId);
            UUID grpId = UUID.fromString(groupId);

            groupService.assignEmployeeToGroup(empId, grpId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee assigned to group successfully");
            response.put("employeeId", employeeId);
            response.put("groupId", groupId);

            log.info("✅ Employee {} assigned to group {} successfully", employeeId, groupId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Assignment error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error assigning employee to group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error assigning employee: " + e.getMessage()));
        }
    }

    /**
     * ✂️ REMOVE EMPLOYEE FROM GROUP
     * Endpoint: DELETE /api/v2/groups/{groupId}/employees/{employeeId}
     */
    @DeleteMapping("/{groupId}/employees/{employeeId}")
    @Operation(summary = "Remove employee from group", description = "Remove an employee from a group")
    public ResponseEntity<Map<String, Object>> removeEmployeeFromGroup(
            @PathVariable String groupId,
            @PathVariable String employeeId) {
        try {
            log.info("✂️ Removing employee {} from group {}", employeeId, groupId);

            UUID empId = UUID.fromString(employeeId);
            UUID grpId = UUID.fromString(groupId);

            groupService.removeEmployeeFromGroup(empId, grpId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee removed from group successfully");
            response.put("employeeId", employeeId);
            response.put("groupId", groupId);

            log.info("✅ Employee {} removed from group {} successfully", employeeId, groupId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Removal error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Error removing employee from group", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error removing employee: " + e.getMessage()));
        }
    }

    // ========== PERMISSION MANAGEMENT ==========

    /**
     * 🔐 GET PERMISSION LEVELS
     * Endpoint: GET /api/v2/groups/permission-levels
     */
    @GetMapping("/permission-levels")
    @Operation(summary = "Get permission levels", description = "Get all available permission levels with descriptions")
    public ResponseEntity<List<GroupDto.PermissionLevelInfo>> getPermissionLevels() {
        try {
            log.info("🔐 Getting permission levels");

            List<GroupDto.PermissionLevelInfo> permissionLevels = groupMapper.getAllPermissionLevels();

            log.debug("✅ Retrieved {} permission levels", permissionLevels.size());
            return ResponseEntity.ok(permissionLevels);

        } catch (Exception e) {
            log.error("❌ Error getting permission levels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 🔍 GET GROUPS BY PERMISSION LEVEL
     * Endpoint: GET /api/v2/groups/permission-level/{level}
     */
    @GetMapping("/permission-level/{level}")
    @Operation(summary = "Get groups by permission level", description = "Get groups with minimum permission level")
    public ResponseEntity<Map<String, Object>> getGroupsByPermissionLevel(@PathVariable int level) {
        try {
            log.info("🔍 Getting groups with permission level >= {}", level);

            if (level < 1 || level > 10) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Permission level must be between 1 and 10"));
            }

            List<Group> groups = groupService.getGroupsByMinimumPermissionLevel(level);
            List<GroupDto.GroupInfo> groupDtos = groupMapper.toGroupInfoList(groups);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupDtos);
            response.put("minimumPermissionLevel", level);
            response.put("totalGroups", groupDtos.size());
            response.put("permissionLevelInfo", groupMapper.getPermissionLevelInfo(level));

            log.debug("✅ Found {} groups with permission level >= {}", groupDtos.size(), level);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting groups by permission level: {}", level, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving groups: " + e.getMessage()));
        }
    }

    // ========== STATISTICS AND REPORTING ==========

    /**
     * 📊 GET GROUP STATISTICS
     * Endpoint: GET /api/v2/groups/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get group statistics", description = "Get comprehensive statistics about groups and their members")
    public ResponseEntity<Map<String, Object>> getGroupStatistics() {
        try {
            log.info("📊 Getting group statistics");

            List<Object[]> rawStats = groupService.getGroupStatistics();
            List<GroupDto.GroupStatistics> stats = groupMapper.toGroupStatisticsList(rawStats);
            List<Group> emptyGroups = groupService.getEmptyGroups();
            List<GroupDto.GroupInfo> emptyGroupDtos = groupMapper.toGroupInfoList(emptyGroups);

            Map<String, Object> response = new HashMap<>();
            response.put("groupStatistics", stats);
            response.put("emptyGroups", emptyGroupDtos);
            response.put("totalGroups", stats.size());
            response.put("emptyGroupsCount", emptyGroupDtos.size());
            response.put("totalEmployeesInGroups", stats.stream()
                    .mapToLong(GroupDto.GroupStatistics::getEmployeeCount)
                    .sum());

            // Add permission level distribution
            Map<Integer, Long> permissionDistribution = stats.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            GroupDto.GroupStatistics::getPermissionLevel,
                            java.util.stream.Collectors.summingLong(GroupDto.GroupStatistics::getEmployeeCount)
                    ));
            response.put("permissionLevelDistribution", permissionDistribution);

            log.debug("✅ Statistics retrieved: {} total groups, {} empty groups",
                    stats.size(), emptyGroupDtos.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error getting group statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving statistics: " + e.getMessage()));
        }
    }

    /**
     * 📈 GET GROUP SUMMARY
     * Endpoint: GET /api/v2/groups/summary
     */
    @GetMapping("/summary")
    @Operation(summary = "Get group summary", description = "Get high-level summary of group system")
    public ResponseEntity<Map<String, Object>> getGroupSummary() {
        try {
            log.info("📈 Getting group summary");

            List<Group> allGroups = groupService.getAllActiveGroups();
            List<Object[]> rawStats = groupService.getGroupStatistics();
            List<Group> emptyGroups = groupService.getEmptyGroups();

            long totalEmployeesInGroups = rawStats.stream()
                    .mapToLong(stat -> ((Number) stat[2]).longValue())
                    .sum();

            Map<String, Object> summary = new HashMap<>();
            summary.put("totalActiveGroups", allGroups.size());
            summary.put("totalEmployeesInGroups", totalEmployeesInGroups);
            summary.put("emptyGroupsCount", emptyGroups.size());
            summary.put("groupsWithEmployeesCount", allGroups.size() - emptyGroups.size());

            // Average employees per group (excluding empty groups)
            double avgEmployeesPerGroup = allGroups.size() > emptyGroups.size() ?
                    (double) totalEmployeesInGroups / (allGroups.size() - emptyGroups.size()) : 0.0;
            summary.put("averageEmployeesPerGroup", Math.round(avgEmployeesPerGroup * 100.0) / 100.0);

            // Permission level breakdown
            Map<Integer, Integer> permissionLevelCounts = allGroups.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            Group::getPermissionLevel,
                            java.util.stream.Collectors.summingInt(g -> 1)
                    ));
            summary.put("groupsByPermissionLevel", permissionLevelCounts);

            // Health indicators
            summary.put("healthIndicators", Map.of(
                    "hasAdminGroups", allGroups.stream().anyMatch(g -> g.getPermissionLevel() >= 8),
                    "hasManagerGroups", allGroups.stream().anyMatch(g -> g.getPermissionLevel() >= 5),
                    "hasProcessorGroups", allGroups.stream().anyMatch(g -> g.getPermissionLevel() <= 3),
                    "emptyGroupsPercentage", allGroups.isEmpty() ? 0.0 :
                            Math.round((double) emptyGroups.size() / allGroups.size() * 10000.0) / 100.0
            ));

            log.debug("✅ Group summary generated successfully");
            return ResponseEntity.ok(summary);

        } catch (Exception e) {
            log.error("❌ Error getting group summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving summary: " + e.getMessage()));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     * 🔧 INITIALIZE DEFAULT GROUPS
     * Endpoint: POST /api/v2/groups/init-defaults
     */
    @PostMapping("/init-defaults")
    @Operation(summary = "Initialize default groups", description = "Create standard groups if they don't exist")
    public ResponseEntity<Map<String, Object>> initializeDefaultGroups() {
        try {
            log.info("🔧 Initializing default groups");

            groupService.initializeDefaultGroups();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default groups initialized successfully");
            response.put("defaultGroups", List.of("ADMIN", "MANAGER", "SUPERVISOR", "PROCESSOR", "VIEWER"));

            log.info("✅ Default groups initialized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error initializing default groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error initializing groups: " + e.getMessage()));
        }
    }

    /**
     * ✅ VALIDATE GROUP NAME
     * Endpoint: POST /api/v2/groups/validate-name
     */
    @PostMapping("/validate-name")
    @Operation(summary = "Validate group name", description = "Validate and suggest group name format")
    public ResponseEntity<Map<String, Object>> validateGroupName(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            log.info("✅ Validating group name: {}", name);

            Map<String, Object> response = new HashMap<>();
            response.put("originalName", name);
            response.put("isValid", groupMapper.isValidGroupName(name));

            if (!groupMapper.isValidGroupName(name)) {
                String suggestedName = groupMapper.suggestGroupNameFormat(name);
                response.put("suggestedName", suggestedName);
                response.put("reason", "Group name must be uppercase with underscores only (A-Z, 0-9, _)");
            }

            // Check if name already exists
            if (groupMapper.isValidGroupName(name)) {
                boolean exists = groupService.findByName(name).isPresent();
                response.put("alreadyExists", exists);
                if (exists) {
                    response.put("reason", "Group name already exists");
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error validating group name", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error validating name: " + e.getMessage()));
        }
    }

    /**
     * 🔍 SEARCH GROUPS
     * Endpoint: POST /api/v2/groups/search
     */
    @PostMapping("/search")
    @Operation(summary = "Advanced group search", description = "Search groups with advanced filters using DTO")
    public ResponseEntity<Map<String, Object>> searchGroups(@Valid @RequestBody GroupDto.GroupSearchRequest searchRequest) {
        try {
            log.info("🔍 Advanced group search: {}", searchRequest.getSearchTerm());

            // For now, use basic search - can be enhanced with more complex filtering
            List<Group> groups;
            if (searchRequest.getSearchTerm() != null && !searchRequest.getSearchTerm().trim().isEmpty()) {
                groups = groupService.searchGroups(searchRequest.getSearchTerm());
            } else {
                groups = groupService.getAllActiveGroups();
            }

            // Apply additional filters
            if (searchRequest.getMinPermissionLevel() != null) {
                groups = groups.stream()
                        .filter(g -> g.getPermissionLevel() >= searchRequest.getMinPermissionLevel())
                        .toList();
            }

            if (searchRequest.getMaxPermissionLevel() != null) {
                groups = groups.stream()
                        .filter(g -> g.getPermissionLevel() <= searchRequest.getMaxPermissionLevel())
                        .toList();
            }

            if (searchRequest.getActive() != null) {
                groups = groups.stream()
                        .filter(g -> g.getActive().equals(searchRequest.getActive()))
                        .toList();
            }

            if (searchRequest.getHasEmployees() != null) {
                if (searchRequest.getHasEmployees()) {
                    groups = groups.stream()
                            .filter(g -> !g.getEmployees().isEmpty())
                            .toList();
                } else {
                    groups = groups.stream()
                            .filter(g -> g.getEmployees().isEmpty())
                            .toList();
                }
            }

            // Apply pagination
            int totalElements = groups.size();
            int page = searchRequest.getPage();
            int size = searchRequest.getSize();
            int start = Math.min(page * size, totalElements);
            int end = Math.min(start + size, totalElements);

            List<Group> paginatedGroups = groups.subList(start, end);
            List<GroupDto.GroupInfo> groupDtos = groupMapper.toGroupInfoList(paginatedGroups);

            Map<String, Object> response = new HashMap<>();
            response.put("groups", groupDtos);
            response.put("totalElements", totalElements);
            response.put("totalPages", (int) Math.ceil((double) totalElements / size));
            response.put("currentPage", page);
            response.put("hasNext", end < totalElements);
            response.put("hasPrevious", page > 0);
            response.put("searchCriteria", searchRequest);

            log.debug("✅ Found {} groups matching search criteria", totalElements);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Error searching groups", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error searching groups: " + e.getMessage()));
        }
    }

    private UUID parseId(String idString) {
        if (idString == null || idString.trim().isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty");
        }

        try {
            String cleanId = idString.trim();

            // Format ULID (26 caractères)
            if (cleanId.length() == 26 && cleanId.matches("[0-9A-Z]+")) {
                Ulid ulid = Ulid.from(cleanId);
                return ulid.toUuid();
            }

            // Format UUID avec tirets (36 caractères)
            if (cleanId.length() == 36 && cleanId.contains("-")) {
                return UUID.fromString(cleanId);
            }

            // Format HEX sans tirets (32 caractères) - pour transition
            if (cleanId.length() == 32 && cleanId.matches("[0-9A-Fa-f]+")) {
                String formatted = cleanId.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            throw new IllegalArgumentException("Invalid ID format: " + cleanId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid ID: " + idString, e);
        }
    }

}