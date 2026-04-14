package com.rabbit.server.repository;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
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

    public void create(TaskDto dto) throws SQLException {
        db.update("""
            INSERT INTO tasks (project_id, assigned_to, created_by, title, description, priority, status, deadline)
            VALUES (?, ?, ?, ?, ?, ?, ?::task_status, ?)
        """,
                dto.getProjectId(),
                dto.getAssignedTo(),
                dto.getCreatedBy(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getPriority(),
                dto.getStatus(),
                dto.getDeadline()
        );
    }

    public boolean update(TaskDto dto) throws SQLException {
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
                dto.getAssignedTo(),
                dto.getTitle(),
                dto.getDescription(),
                dto.getPriority(),
                dto.getStatus(),
                dto.getDeadline(),
                dto.getId()
        ) > 0;
    }

    public boolean delete(int taskId) throws SQLException {
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
        dto.setAssignedTo((int) row.get("assigned_to"));
        dto.setCreatedBy((int) row.get("created_by"));
        dto.setTitle((String) row.get("title"));
        dto.setDescription((String) row.get("description"));
        dto.setPriority((int) row.get("priority"));
        dto.setStatus(row.get("status").toString());
        dto.setDeadline((java.sql.Timestamp) row.get("deadline"));
        return dto;
    }
}