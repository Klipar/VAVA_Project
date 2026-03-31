package com.rabbit.server.repository;

import com.rabbit.common.dto.ProjectDto;
import com.rabbit.server.service.DatabaseService;

import java.sql.SQLException;
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

    public Optional<ProjectDto> findById(int projectId) throws SQLException {
        return db.query("SELECT * FROM project WHERE id = ?", projectId)
                .stream()
                .map(this::mapToDto)
                .findFirst();
    }

    public void create(ProjectDto dto) throws SQLException {
        db.update("""
            INSERT INTO project (title, description, deadline, status)
            VALUES (?, ?, ?, ?::project_status)
        """,
                dto.getTitle(),
                dto.getDescription(),
                dto.getDeadline(),
                dto.getStatus()
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

    private ProjectDto mapToDto(Map<String, Object> row) {
        ProjectDto dto = new ProjectDto();
        dto.setId((int) row.get("id"));
        dto.setTitle((String) row.get("title"));
        dto.setDescription((String) row.get("description"));
        dto.setDeadline((java.sql.Timestamp) row.get("deadline"));
        dto.setStatus(row.get("status").toString());
        return dto;
    }
}