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

    public List<ProjectDto> getProjectsForUser(int userId) throws SQLException {
        return repo.findAllByUserId(userId);
    }

    public Optional<ProjectDto> getProjectById(int projectId, int requesterId) throws SQLException {
        if (!repo.isProjectMember(requesterId, projectId)) {
            throw new SecurityException("Access denied: you are not a member of this project");
        }
        return repo.findById(projectId);
    }

    public ProjectDto createProject(int requesterId, ProjectDto dto) throws SQLException {
        int projectId = repo.create(dto, requesterId);
        return repo.findById(projectId).orElseThrow(() -> new SQLException("Failed to retrieve created project"));
    }

    public ProjectDto updateProject(int projectId, int requesterId, ProjectDto dto) throws SQLException {
        Optional<ProjectDto> existing = repo.findById(projectId);
        if (existing.isEmpty()) throw new IllegalArgumentException("Project not found");
        if (!repo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can update project");
        }
        dto.setId(projectId);
        repo.update(dto);
        return repo.findById(projectId).orElseThrow();
    }

    public void deleteProject(int projectId, int requesterId) throws SQLException {
        Optional<ProjectDto> existing = repo.findById(projectId);
        if (existing.isEmpty()) throw new IllegalArgumentException("Project not found");
        if (!repo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can delete project");
        }
        repo.delete(projectId);
    }
}
