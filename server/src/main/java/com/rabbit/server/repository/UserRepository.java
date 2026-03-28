package com.rabbit.server.repository;

import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        String sql = "SELECT id, name, nickname, email, role, created_at FROM users WHERE id = ?";
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
     * Get UserDTO by username (email) and password hash.
     * @param email user's email (username)
     * @param passwordHash hashed password
     * @return Optional containing UserDTO if credentials match
     */
    public Optional<UserDto> findByEmailAndPasswordHash(String email, String passwordHash) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM users WHERE email = ? AND password_hash = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, email, passwordHash);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email and password", e);
        }
    }

    /**
     * Get UserDTO by password hash.
     * @param passwordHash hashed password
     * @return Optional containing UserDTO if found
     */
    public Optional<UserDto> findByPasswordHash(String passwordHash) {
        String sql = "SELECT id, name, nickname, email, role, created_at FROM users WHERE password_hash = ?";
        try {
            List<Map<String, Object>> results = dbService.query(sql, passwordHash);

            if (results.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(mapToUserDto(results.get(0)));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by password hash", e);
        }
    }

    /**
     * Save UserDTO to database.
     * @param userDto UserDTO object (without id)
     * @param passwordHash hashed password
     * @return generated user ID
     */
    public Long save(UserDto userDto, String passwordHash) {
        String sql = "INSERT INTO users (name, nickname, email, password_hash, role) VALUES (?, ?, ?, ?, ?)";

        try {
            return dbService.insertAndGetId(
                    sql,
                    userDto.getName(),
                    userDto.getNickname(),
                    userDto.getEmail(),
                    passwordHash,
                    userDto.getRole().name()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + userDto.getEmail(), e);
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
        dto.setRole(UserRole.valueOf(roleStr));

        Timestamp timestamp = (Timestamp) row.get("created_at");
        if (timestamp != null) {
            dto.setCreatedAt(timestamp.toLocalDateTime());
        }

        return dto;
    }
}