package com.rabbit.server.repository;

import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.enums.ProjectStatus;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProjectRepository {

    private final DatabaseService db = DatabaseService.getInstance();

    public List<ProjectDto> findAll() throws SQLException {
        return db.query("SELECT * FROM projects")
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<ProjectDto> findAllByUserId(int userId) throws SQLException {
        return db.query("""
            SELECT p.* FROM projects p
            JOIN user_projects up ON up.project_id = p.id
            WHERE up.user_id = ?
        """, userId)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public Optional<ProjectDto> findById(int projectId) throws SQLException {
        return db.query("SELECT * FROM projects WHERE id = ?", projectId)
                .stream()
                .map(this::mapToDto)
                .findFirst();
    }

    public int create(ProjectDto dto, int creatorId) throws SQLException {
        List<Map<String, Object>> result = db.query("""
            INSERT INTO projects (title, description, deadline, status)
            VALUES (?, ?, ?, ?::project_status)
            RETURNING id
        """,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline(),
                dto.getStatus().getValue()
        );
        int projectId = ((Number) result.getFirst().get("id")).intValue();
        db.update(
                "INSERT INTO user_projects (user_id, project_id, role) VALUES (?, ?, 'master'::project_user_role)",
                creatorId, projectId
        );
        return projectId;
    }

    public boolean update(ProjectDto dto) throws SQLException {
        return db.update("""
            UPDATE projects SET
                title = ?,
                description = ?,
                deadline = ?,
                status = ?::project_status
            WHERE id = ?
        """,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline(),
                dto.getStatus().getValue(),
                dto.getId()
        ) > 0;
    }

    public boolean delete(int projectId) throws SQLException {
        return db.update("DELETE FROM projects WHERE id = ?", projectId) > 0;
    }

    public boolean isProjectAdmin(int userId, int projectId) throws SQLException {
        return !db.query(
                "SELECT 1 FROM user_projects WHERE user_id = ? AND project_id = ? AND role = 'master'",
                userId, projectId
        ).isEmpty();
    }

    public boolean isProjectMember(int userId, int projectId) throws SQLException {
        return !db.query(
                "SELECT 1 FROM user_projects WHERE user_id = ? AND project_id = ?",
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
        String statusStr = row.get("status").toString().toUpperCase();
        dto.setStatus(ProjectStatus.valueOf(statusStr));
        return dto;
    }
}