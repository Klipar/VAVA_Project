package com.rabbit.server.service;

import com.rabbit.common.dto.ProjectDto;
import com.rabbit.server.repository.ProjectRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ProjectService {
    private final ProjectRepository repo = new ProjectRepository();

    public List<ProjectDto> getAllProjects() throws SQLException {
        return repo.findAll();
    }

    public Optional<ProjectDto> getProjectById(int projectId) throws SQLException {
        return repo.findById(projectId);
    }

    public void createProject(int requesterId, ProjectDto dto) throws SQLException {
        repo.create(dto, requesterId);
    }

    public boolean updateProject(int projectId, int requesterId, ProjectDto dto) throws SQLException {
        Optional<ProjectDto> existing = repo.findById(projectId);
        if (existing.isEmpty()) return false;
        if (!repo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can update project");
        }
        dto.setId(projectId);
        return repo.update(dto);
    }

    public boolean deleteProject(int projectId, int requesterId) throws SQLException {
        Optional<ProjectDto> existing = repo.findById(projectId);
        if (existing.isEmpty()) return false;
        if (!repo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can delete project");
        }
        return repo.delete(projectId);
    }
}
