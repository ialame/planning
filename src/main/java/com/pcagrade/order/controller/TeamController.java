package com.pcagrade.order.controller;

import com.pcagrade.order.dto.TeamDto;
import com.pcagrade.order.entity.Employee;
import com.pcagrade.order.entity.Team;
import com.pcagrade.order.repository.EmployeeRepository;
import com.pcagrade.order.service.EmployeeService;
import com.pcagrade.order.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Team Controller - Role Management API
 * RESTful API for managing teams and roles
 */
@RestController
@RequestMapping("/api/v2/teams")
@Tag(name = "Team Management", description = "API for managing teams and roles")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173", "http://127.0.0.1:3000"})
public class TeamController {
    private static final Logger log = LoggerFactory.getLogger(TeamController.class);

    @Autowired
    private TeamService teamService;

    @Autowired
    private EmployeeRepository employeeRepository;

    // ========== CRUD OPERATIONS ==========

    /**
     *  GET ALL TEAMS
     * Endpoint: GET /api/teams
     */
    @GetMapping
    @Operation(summary = "Get all active teams", description = "Retrieve all active teams with optional pagination")
    @ApiResponse(responseCode = "200", description = "Teams retrieved successfully")
    public ResponseEntity<Map<String, Object>> getAllTeams(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search) {

        try {
            log.info(" Getting teams - page: {}, size: {}, sortBy: {}, search: '{}'",
                    page, size, sortBy, search);

            Map<String, Object> response = new HashMap<>();

            if (page < 0 || size <= 0) {
                // Return all teams without pagination
                List<Team> teams;
                if (search != null && !search.trim().isEmpty()) {
                    teams = teamService.searchTeams(search.trim());
                } else {
                    teams = teamService.getAllActiveTeams();
                }

                response.put("teams", teams);
                response.put("totalElements", teams.size());
                response.put("totalPages", 1);
                response.put("currentPage", 0);
                response.put("hasNext", false);
                response.put("hasPrevious", false);
            } else {
                // Return paginated results
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<Team> teamPage;
                if (search != null && !search.trim().isEmpty()) {
                    teamPage = teamService.searchTeamsPaginated(search.trim(), pageable);
                } else {
                    teamPage = teamService.getAllActiveTeamsPaginated(pageable);
                }

                response.put("teams", teamPage.getContent());
                response.put("totalElements", teamPage.getTotalElements());
                response.put("totalPages", teamPage.getTotalPages());
                response.put("currentPage", teamPage.getNumber());
                response.put("hasNext", teamPage.hasNext());
                response.put("hasPrevious", teamPage.hasPrevious());
            }

            log.debug(" Retrieved {} teams", response.get("totalElements"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error retrieving teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving teams: " + e.getMessage()));
        }
    }

    /**
     *  GET TEAM BY ID
     * Endpoint: GET /api/teams/{id}
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get team by ID", description = "Retrieve a single team by its ID")
    @ApiResponse(responseCode = "200", description = "Team found")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> getTeamById(@PathVariable String id) {
        try {
            log.info(" Getting team by ID: {}", id);

            UUID teamId = UUID.fromString(id);
            Optional<Team> teamOpt = Optional.ofNullable(teamService.getTeamById(teamId));

            if (teamOpt.isPresent()) {
                Team team = teamOpt.get();
                Map<String, Object> response = new HashMap<>();
                response.put("team", team);
                response.put("employeeCount", teamService.countActiveEmployeesInTeam(teamId));

                log.debug(" Team found: {}", team.getName());
                return ResponseEntity.ok(response);
            } else {
                log.warn(" Team not found: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid team ID format: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid team ID format"));
        } catch (Exception e) {
            log.error(" Error getting team by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving team: " + e.getMessage()));
        }
    }

    /**
     *  CREATE NEW TEAM
     * Endpoint: POST /api/teams
     */
    @PostMapping
    @Operation(summary = "Create new team", description = "Create a new team/role")
    @ApiResponse(responseCode = "201", description = "Team created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid team data")
    public ResponseEntity<Map<String, Object>> createTeam(@Valid @RequestBody Team team) {
        try {
            log.info(" Creating new team: {}", team.getName());

            Team createdTeam = teamService.createTeam(team);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team created successfully");
            response.put("team", createdTeam);

            log.info(" Team created successfully: {} (ID: {})",
                    createdTeam.getName(), createdTeam.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid team data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(" Error creating team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating team: " + e.getMessage()));
        }
    }

    /**
     *  UPDATE TEAM
     * Endpoint: PUT /api/teams/{id}
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update team", description = "Update an existing team")
    @ApiResponse(responseCode = "200", description = "Team updated successfully")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> updateTeam(
            @PathVariable String id,
            @Valid @RequestBody Team team) {
        try {
            log.info(" Updating team: {}", id);

            UUID teamId = UUID.fromString(id);
            Team updatedTeam = teamService.updateTeam(teamId, team);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team updated successfully");
            response.put("team", updatedTeam);

            log.info(" Team updated successfully: {}", updatedTeam.getName());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid team ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (NoSuchElementException e) {
            log.warn(" Team not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error(" Error updating team: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating team: " + e.getMessage()));
        }
    }

    /**
     *  DELETE TEAM
     * Endpoint: DELETE /api/teams/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete team", description = "Delete a team (soft delete)")
    @ApiResponse(responseCode = "200", description = "Team deleted successfully")
    @ApiResponse(responseCode = "404", description = "Team not found")
    public ResponseEntity<Map<String, Object>> deleteTeam(@PathVariable String id) {
        try {
            log.info(" Deleting team: {}", id);

            UUID teamId = UUID.fromString(id);
            Team teamOpt = teamService.getTeamById(teamId);

            if (teamOpt==null) {
                log.warn(" Team not found for deletion: {}", id);
                return ResponseEntity.notFound().build();
            }

            teamService.deleteTeam(teamId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team deleted successfully");

            log.info(" Team deleted successfully: {}", id);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid team ID: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(" Error deleting team: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error deleting team: " + e.getMessage()));
        }
    }

    // ========== EMPLOYEE-TEAM MANAGEMENT ==========


    /**
     *  ASSIGN EMPLOYEE TO TEAM
     * Endpoint: POST /api/teams/assign
     */
    @PostMapping("/assign")
    @Operation(summary = "Assign employee to team", description = "Add an employee to a team")
    public ResponseEntity<Map<String, Object>> assignEmployeeToTeam(
            @Parameter(description = "Employee ID") @RequestParam String employeeId,
            @Parameter(description = "Team ID") @RequestParam String teamId) {
        try {
            log.info(" Assigning employee {} to team {}", employeeId, teamId);

            UUID empId = UUID.fromString(employeeId);
            UUID tmId = UUID.fromString(teamId);

            teamService.assignEmployeeToTeam(empId, tmId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee assigned to team successfully");

            log.info(" Employee assigned successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid ID format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(" Error assigning employee to team", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error assigning employee: " + e.getMessage()));
        }
    }

//    /**
//     *  REMOVE EMPLOYEE FROM TEAM
//     * Endpoint: DELETE /api/teams/remove
//     */
//    @DeleteMapping("/remove")
//    @Operation(summary = "Remove employee from team", description = "Remove an employee from a team")
//    public ResponseEntity<Map<String, Object>> removeEmployeeFromTeam(
//            @Parameter(description = "Employee ID") @RequestParam String employeeId,
//            @Parameter(description = "Team ID") @RequestParam String teamId) {
//        try {
//            log.info(" Removing employee {} from team {}", employeeId, teamId);
//
//            UUID empId = UUID.fromString(employeeId);
//            UUID tmId = UUID.fromString(teamId);
//
//            teamService.removeEmployeeFromTeam(empId, tmId);
//
//            Map<String, Object> response = new HashMap<>();
//            response.put("success", true);
//            response.put("message", "Employee removed from team successfully");
//
//            log.info(" Employee removed successfully");
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            log.warn(" Invalid ID format: {}", e.getMessage());
//            return ResponseEntity.badRequest()
//                    .body(Map.of("error", e.getMessage()));
//        } catch (Exception e) {
//            log.error(" Error removing employee from team", e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("error", "Error removing employee: " + e.getMessage()));
//        }
//    }

    /**
     *  UPDATE EMPLOYEE TEAMS
     * Endpoint: PUT /api/teams/employee/{employeeId}/teams
     */
    @PutMapping("/employee/{employeeId}/teams")
    @Operation(summary = "Update employee teams", description = "Replace all teams for an employee")
    public ResponseEntity<Map<String, Object>> updateEmployeeTeams(
            @PathVariable String employeeId,
            @RequestBody List<String> teamIds) {
        try {
            log.info(" Updating teams for employee: {}", employeeId);

            UUID empId = UUID.fromString(employeeId);
//            List<UUID> tmIds = teamIds.stream()
//                    .map(UUID::fromString)
//                    .toList();

            teamService.updateEmployeeTeams(empId, teamIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Employee teams updated successfully");
            response.put("updatedTeamCount", teamIds.size());

            log.info(" Updated {} teams for employee {}", teamIds.size(), employeeId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn(" Invalid ID format: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error(" Error updating employee teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error updating teams: " + e.getMessage()));
        }
    }

    // ========== UTILITY ENDPOINTS ==========

    /**
     *  INITIALIZE DEFAULT TEAMS
     * Endpoint: POST /api/teams/init-defaults
     */
    @PostMapping("/init-defaults")
    @Operation(summary = "Initialize default teams", description = "Create standard teams if they don't exist")
    public ResponseEntity<Map<String, Object>> initializeDefaultTeams() {
        try {
            log.info(" Initializing default teams");

            teamService.initializeDefaultTeams();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Default teams initialized successfully");

            log.info(" Default teams initialized successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error initializing default teams", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error initializing teams: " + e.getMessage()));
        }
    }

    /**
     *  GET TEAM STATISTICS
     * Endpoint: GET /api/teams/statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get team statistics", description = "Get statistics about teams and their members")
    public ResponseEntity<Map<String, Object>> getTeamStatistics() {
        try {
            log.info(" Getting team statistics");

            List<Object[]> stats = teamService.getTeamStatistics();
            List<Team> emptyTeams = teamService.getEmptyTeams();

            Map<String, Object> response = new HashMap<>();
            response.put("teamStatistics", stats);
            response.put("emptyTeams", emptyTeams);
            response.put("totalTeams", stats.size());
            response.put("emptyTeamsCount", emptyTeams.size());

            log.debug(" Statistics retrieved: {} total teams, {} empty teams",
                    stats.size(), emptyTeams.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error(" Error getting team statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error retrieving statistics: " + e.getMessage()));
        }
    }


    // ========== EMPLOYEE MANAGEMENT ENDPOINTS ==========

    /**
     * Add an employee to a team
     * POST /api/v2/teams/{teamId}/employees/{employeeId}
     */
    //  TeamController.java, line ~145

    @PostMapping("/{teamId}/employees/{employeeId}")
    public ResponseEntity<?> addEmployeeToTeam(
            @PathVariable String teamId,
            @PathVariable String employeeId) {
        try {
            log.info(" Adding employee {} to team {}", employeeId, teamId);
            UUID teamUuid = parseUUID(teamId);
            UUID employeeUuid = parseUUID(employeeId);

            //  CORRECT THE ORDER: teamId THEN employeeId
            teamService.assignEmployeeToTeam(teamUuid, employeeUuid);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Employee added to team successfully"
            ));
        } catch (Exception e) {
            log.error(" Error adding employee to team", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Remove an employee from a team
     * DELETE /api/v2/teams/{teamId}/employees/{employeeId}
     */
    @DeleteMapping("/{teamId}/employees/{employeeId}")
    public ResponseEntity<?> removeEmployeeFromTeam(
            @PathVariable String teamId,
            @PathVariable String employeeId) {
        try {
            log.info(" Removing employee {} from team {}", employeeId, teamId);
            UUID teamUuid = parseUUID(teamId);
            UUID employeeUuid = parseUUID(employeeId);

            //  CORRECT THE ORDER: teamId THEN employeeId
            teamService.removeEmployeeFromTeam(teamUuid, employeeUuid);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Employee removed from team successfully"
            ));
        } catch (Exception e) {
            log.error(" Error removing employee from team", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
    /**
     * Parse UUID with or without hyphens
     * IMPORTANT: Do not reformat if already correct!
     */
    private UUID parseUUID(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            throw new IllegalArgumentException("UUID cannot be empty");
        }

        try {
            // Try parsing directly (if already using dashes)
            return UUID.fromString(uuidString);
        } catch (IllegalArgumentException e) {
            // Si ça échoue, c'est probablement sans tirets
            // Nettoyer et ajouter les tirets
            String clean = uuidString.replace("-", "").toLowerCase();

            if (clean.length() != 32) {
                throw new IllegalArgumentException("Invalid UUID format: " + uuidString);
            }

            String formatted = String.format(
                    "%s-%s-%s-%s-%s",
                    clean.substring(0, 8),
                    clean.substring(8, 12),
                    clean.substring(12, 16),
                    clean.substring(16, 20),
                    clean.substring(20, 32)
            );

            return UUID.fromString(formatted);
        }
    }



    /**
     * Get all employees in a team
     * GET /api/v2/teams/{teamId}/employees
     */
    @GetMapping("/{teamId}/employees")
    public ResponseEntity<?> getTeamEmployees(@PathVariable String teamId) {
        try {
            List<?> employees = teamService.getTeamEmployees(UUID.fromString(teamId));
            return ResponseEntity.ok(employees);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all teams for an employee
     * GET /api/v2/teams/employee/{employeeId}
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeTeams(@PathVariable String employeeId) {
        try {
            UUID employeeUuid = UUID.fromString(employeeId);
            List<Team> teams = teamService.getTeamsByEmployeeId(employeeUuid);

            return ResponseEntity.ok(Map.of("teams", teams));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/employee/{employeeId}/bulk")
    public ResponseEntity<?> bulkUpdateEmployeeTeams(
            @PathVariable String employeeId,
            @RequestBody Map<String, Object> payload) {
        try {
            List<String> teamIds = (List<String>) payload.get("teamIds");

            //  Convert employeeId String  UUID, keep teamIds en List<String>
            teamService.updateEmployeeTeams(UUID.fromString(employeeId), teamIds);

            return ResponseEntity.ok().body(Map.of("message", "Employee teams updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
