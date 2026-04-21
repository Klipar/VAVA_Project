package com.rabbit.server.repository;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.dto.TaskRequestDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TaskRepository {

    private final DatabaseService db = DatabaseService.getInstance();

    public List<TaskDto> findByProjectId(int projectId) throws SQLException {
        return db.query("SELECT * FROM tasks WHERE project_id = ?", projectId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<TaskDto> findById(int taskId) throws SQLException {
        return db.query("SELECT * FROM tasks WHERE id = ?", taskId)
                .stream()
                .map(this::mapToDto)
                .findFirst();
    }

    public boolean updateStatus(int taskId, String newStatus) throws SQLException {
        return db.update("""
        UPDATE tasks SET
            status = ?::task_status 
        WHERE id = ?
    """,
                newStatus,
                taskId
        ) > 0;
    }

    public int create(int projectId, int createdBy, TaskRequestDto dto) throws SQLException {
        List<Map<String, Object>> result = db.query("""
        INSERT INTO tasks (project_id, assigned_to, created_by, title, description, priority, status, deadline)
        VALUES (?, ?, ?, ?, ?, ?, ?::task_status, ?)
        RETURNING id
    """,
                projectId,
                dto.getAssignedTo() == 0 ? null : dto.getAssignedTo(),
                createdBy,
                dto.getTitle(),
                dto.getDescription(),
                dto.getPriority(),
                dto.getStatus(),
                parseDeadline(dto.getDeadline())
        );
        return ((Number) result.getFirst().get("id")).intValue();
    }

    public boolean update(int taskId, TaskRequestDto dto) throws SQLException {
        return db.update("""
        UPDATE tasks SET
            assigned_to = ?,
            title = ?,
            description = ?,
            priority = ?,
            status = ?::task_status,
            deadline = ?
        WHERE id = ?
    """,
                dto.getAssignedTo() == 0 ? null : dto.getAssignedTo(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getPriority(),
                dto.getStatus(),
                parseDeadline(dto.getDeadline()),
                taskId
        ) > 0;
    }

    public boolean delete(int taskId, int userId) throws SQLException {
        return db.update("DELETE FROM tasks WHERE id = ?", taskId) > 0;
    }

    public boolean isProjectAdmin(int userId, int projectId) throws SQLException {
        return !db.query(
                "SELECT 1 FROM user_projects WHERE user_id = ? AND project_id = ? AND role = 'master'",
                userId, projectId
        ).isEmpty();
    }

    private TaskDto mapToDto(Map<String, Object> row) {
        TaskDto dto = new TaskDto();
        dto.setId((int) row.get("id"));
        dto.setProjectId((int) row.get("project_id"));
        Object assignedTo = row.get("assigned_to");
        dto.setAssignedTo(assignedTo != null ? (int) assignedTo : 0);
        dto.setCreatedBy((int) row.get("created_by"));
        dto.setTitle((String) row.get("title"));
        dto.setDescription((String) row.get("description"));
        dto.setPriority((int) row.get("priority"));
        dto.setStatus(row.get("status").toString());
        Timestamp ts = (Timestamp) row.get("deadline");
        dto.setDeadline(ts != null ? ts.toLocalDateTime().toString() : null);
        return dto;
    }

    private Timestamp parseDeadline(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return null;
        }

        dateString = dateString.trim();

        try {
            if (dateString.matches("-?\\d+")) {
                long timestamp = Long.parseLong(dateString);
                if (timestamp < 0) return null;
                if (String.valueOf(timestamp).length() == 10) {
                    return new Timestamp(timestamp * 1000);
                }
                return new Timestamp(timestamp);
            }

            if (dateString.matches("-?\\d+\\.\\d+")) {
                double timestamp = Double.parseDouble(dateString);
                if (timestamp < 0) return null;
                return new Timestamp((long) (timestamp * 1000));
            }

            DateTimeFormatter[] formatters = {
                    DateTimeFormatter.ISO_DATE_TIME,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
                    DateTimeFormatter.ISO_ZONED_DATE_TIME,
                    DateTimeFormatter.ISO_INSTANT,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                    DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
                    DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                    DateTimeFormatter.RFC_1123_DATE_TIME
            };

            LocalDateTime dateTime = null;
            for (DateTimeFormatter formatter : formatters) {
                try {
                    dateTime = LocalDateTime.parse(dateString, formatter);
                    break;
                } catch (DateTimeParseException e) {
                }
            }

            if (dateTime == null) {
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString);
                    dateTime = zonedDateTime.toLocalDateTime();
                } catch (DateTimeParseException e) {
                    try {
                        Instant instant = Instant.parse(dateString);
                        dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    } catch (DateTimeParseException e2) {
                        throw new DateTimeParseException("Unsupported date format: " + dateString, dateString, 0);
                    }
                }
            }

            return Timestamp.valueOf(dateTime);

        } catch (Exception e) {
            System.err.println("Failed to parse deadline: " + dateString + " - " + e.getMessage());
            return null;
        }
    }
}