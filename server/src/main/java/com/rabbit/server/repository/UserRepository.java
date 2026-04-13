package com.rabbit.server.repository;

import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Repository for User database operations.
 * Uses DatabaseService singleton for all database interactions.
 */
public class UserRepository {
    private final DatabaseService dbService;

    public UserRepository(DatabaseService databaseService) {
        this.dbService = databaseService;
    }

    /**
     * Get UserDTO by ID.
     * @param id user ID
     * @return Optional containing UserDTO if found
     */
    public Optional<UserDto> findById(Long id) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM \"user\" WHERE id = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, id);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id: " + id, e);
        }
    }

    /**
     * Get all users from a specific project.
     * @param projectId project ID
     * @return list of UserDto
     */
    public List<UserDto> findAllByProjectId(Long projectId) {
        String sql = "SELECT u.id, u.name, u.nickname, u.email, u.role, u.created_at " +
                "FROM \"user\" u " +
                "INNER JOIN user_project up ON u.id = up.user_id " +
                "WHERE up.project_id = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, projectId);
            return results.stream()
                    .map(this::mapToUserDto)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find users by project id: " + projectId, e);
        }
    }

    /**
     * Update user information.
     * @param userDto user with updated data
     */
    public void update(UserDto userDto) {
        String sql = "UPDATE \"user\" SET name = ?, nickname = ?, email = ? WHERE id = ?";
        try {
            dbService.update(sql,
                    userDto.getName(),
                    userDto.getNickname(),
                    userDto.getEmail(),
                    userDto.getId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + userDto.getId(), e);
        }
    }

    /**
     * Delete user by ID.
     * @param id user ID
     */
    public void deleteById(Long id) {
        String sql = "DELETE FROM \"user\" WHERE id = ?";
        try {
            dbService.update(sql, id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete user: " + id, e);
        }
    }

    /**
     * Find user by email.
     * @param email user email
     * @return Optional containing UserDTO if found
     */
    public Optional<UserDto> findByEmail(String email) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM \"user\" WHERE email = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, email);
            if (results.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email: " + email, e);
        }
    }

    /**
     * Get UserDTO by username (email) and password.
     * @param email user's email (username)
     * @param password hashed password
     * @return Optional containing UserDTO if credentials match
     */
    public Optional<UserDto> findByEmailAndPassword(String email, String password) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM \"user\" WHERE email = ? AND password = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, email, password);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email and password", e);
        }
    }

    /**
     * Get UserDTO by password.
     * @param password hashed password
     * @return Optional containing UserDTO if found
     */
    public Optional<UserDto> findByPassword(String password) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM \"user\" WHERE password = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, password);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by password", e);
        }
    }

    /**
     * Save UserDTO to database.
     * @param userDto UserDTO object (without id)
     * @param password hashed password
     * @return generated user ID
     */
    public Long save(UserDto userDto, String password) {
        String sql = "INSERT INTO \"user\" (name, nickname, email, password, role) VALUES (?, ?, ?, ?, CAST(? AS user_role))";

        try {
            return dbService.insertAndGetId(
                    sql,
                    userDto.getName(),
                    userDto.getNickname(),
                    userDto.getEmail(),
                    password,
                    userDto.getRole().name().toLowerCase()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + userDto.getEmail(), e);
        }
    }

    /**
     * Add user to project.
     * @param userId user ID
     * @param projectId project ID
     */
    public void addUserToProject(Long userId, Long projectId) {
        // Use the correct syntax for enums in PostgreSQL
        String sql = "INSERT INTO user_project (project_id, user_id, role) VALUES (?, ?, CAST(? AS project_user_role))";
        try {
            dbService.update(sql, projectId, userId, "slave");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user " + userId + " to project " + projectId, e);
        }
    }

    /**
     * Add user to project with specific role.
     * @param userId user ID
     * @param projectId project ID
     * @param role role in project ('master' or 'slave')
     */
    public void addUserToProjectWithRole(Long userId, Long projectId, String role) {
        String sql = "INSERT INTO user_project (project_id, user_id, role) VALUES (?, ?, CAST(? AS project_user_role))";
        try {
            dbService.update(sql, projectId, userId, role);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to add user " + userId + " to project " + projectId + " with role " + role, e);
        }
    }

    /**
     * Remove user from specific project.
     * @param userId user ID
     * @param projectId project ID
     */
    public void removeUserFromProject(Long userId, Long projectId) {
        String sql = "DELETE FROM user_project WHERE user_id = ? AND project_id = ?";
        try {
            dbService.update(sql, userId, projectId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove user " + userId + " from project " + projectId, e);
        }
    }

    /**
     * Remove user from all projects.
     * @param userId user ID
     */
    public void removeUserFromAllProjects(Long userId) {
        String sql = "DELETE FROM user_project WHERE user_id = ?";
        try {
            dbService.update(sql, userId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove user " + userId + " from all projects", e);
        }
    }

    /**
     * Check if user belongs to project.
     * @param userId user ID
     * @param projectId project ID
     * @return true if user belongs to project
     */
    public boolean isUserInProject(Long userId, Long projectId) {
        String sql = "SELECT COUNT(*) FROM user_project WHERE user_id = ? AND project_id = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, userId, projectId);
            if (results.isEmpty()) {
                return false;
            }
            long count = (long) results.get(0).get("count");
            return count > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check if user " + userId + " is in project " + projectId, e);
        }
    }

    /**
     * Get all project IDs for a user.
     * @param userId user ID
     * @return list of project IDs
     */
    public List<Long> findAllProjectIdsByUserId(Long userId) {
        String sql = "SELECT project_id FROM user_project WHERE user_id = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, userId);
            return results.stream()
                    .map(row -> ((Number) row.get("project_id")).longValue())
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find project ids for user: " + userId, e);
        }
    }

    /**
     * Helper method to convert database row to UserDto.
     */
    private UserDto mapToUserDto(Map<String, Object> row) {
        UserDto dto = new UserDto();

        dto.setId(((Number) row.get("id")).longValue());
        dto.setName((String) row.get("name"));
        dto.setNickname((String) row.get("nickname"));
        dto.setEmail((String) row.get("email"));

        String roleStr = (String) row.get("role");
        dto.setRole(UserRole.valueOf(roleStr.toUpperCase()));

        Timestamp timestamp = (Timestamp) row.get("created_at");
        if (timestamp != null) {
            dto.setCreatedAt(timestamp.toLocalDateTime());
        }

        return dto;
    }
}