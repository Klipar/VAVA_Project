package com.rabbit.server.repository;

import com.rabbit.common.dto.ProjectDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProjectRepository {

    private final DatabaseService db = DatabaseService.getInstance();

    public List<ProjectDto> findAll() throws SQLException {
        return db.query("SELECT * FROM project")
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProjectDto> findAllByUserId(int userId) throws SQLException {
        return db.query("""
            SELECT p.* FROM project p
            JOIN user_project up ON up.project_id = p.id
            WHERE up.user_id = ?
        """, userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<ProjectDto> findById(int projectId) throws SQLException {
        return db.query("SELECT * FROM project WHERE id = ?", projectId)
                .stream()
                .map(this::mapToDto)
                .findFirst();
    }

    public void create(ProjectDto dto, int creatorId) throws SQLException {
        List<Map<String, Object>> result = db.query("""
            INSERT INTO project (title, description, deadline, status)
            VALUES (?, ?, ?, ?::project_status)
            RETURNING id
        """,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline(),
                dto.getStatus()
        );
        int projectId = (int) result.getFirst().get("id");
        db.update(
                "INSERT INTO user_project (user_id, project_id, role) VALUES (?, ?, 'master'::project_user_role)",
                creatorId, projectId
        );
    }

    public boolean update(ProjectDto dto) throws SQLException {
        return db.update("""
            UPDATE project SET
                title = ?,
                description = ?,
                deadline = ?,
                status = ?::project_status
            WHERE id = ?
        """,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline(),
                dto.getStatus(),
                dto.getId()
        ) > 0;
    }

    public boolean delete(int projectId) throws SQLException {
        return db.update("DELETE FROM project WHERE id = ?", projectId) > 0;
    }

    public boolean isProjectAdmin(int userId, int projectId) throws SQLException {
        return !db.query(
                "SELECT 1 FROM user_project WHERE user_id = ? AND project_id = ? AND role = 'master'",
                userId, projectId
        ).isEmpty();
    }

    private ProjectDto mapToDto(Map<String, Object> row) {
        ProjectDto dto = new ProjectDto();
        dto.setId((int) row.get("id"));
        dto.setTitle((String) row.get("title"));
        dto.setDescription((String) row.get("description"));
        Timestamp ts = (Timestamp) row.get("deadline");
        dto.setDeadline(ts != null ? ts.toLocalDateTime() : null);
        dto.setStatus(row.get("status").toString());
        return dto;
    }
}