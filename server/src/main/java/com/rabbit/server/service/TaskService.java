package com.rabbit.server.service;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.dto.TaskRequestDto;
import com.rabbit.server.repository.TaskRepository;
import com.rabbit.server.repository.ProjectRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TaskService {
    private final TaskRepository taskRepo = new TaskRepository();
    private final ProjectRepository projectRepo = new ProjectRepository();

    public List<TaskDto> getTasksByProjectId(int projectId, int requesterId) throws SQLException {
        if (!projectRepo.isProjectMember(requesterId, projectId)) {
            throw new SecurityException("Access denied: you are not a member of this project");
        }
        return taskRepo.findByProjectId(projectId);
    }

    public TaskDto createTask(int projectId, int requesterId, TaskRequestDto dto) throws SQLException {
        if (!projectRepo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can create tasks");
        }
        int taskId = taskRepo.create(projectId, requesterId, dto);
        return taskRepo.findById(taskId).orElseThrow(() -> new SQLException("Failed to retrieve created task"));
    }

    public TaskDto updateTask(int taskId, int requesterId, TaskRequestDto dto) throws SQLException {
        Optional<TaskDto> existing = taskRepo.findById(taskId);
        if (existing.isEmpty()) return null;
        if (!projectRepo.isProjectAdmin(requesterId, existing.get().getProjectId())) {
            throw new SecurityException("Only project admin can update tasks");
        }
        boolean updated = taskRepo.update(taskId, dto);
        if (!updated) return null;
        return taskRepo.findById(taskId).orElse(null);
    }

    public TaskDto deleteTask(int taskId, int requesterId) throws SQLException {
        Optional<TaskDto> existing = taskRepo.findById(taskId);
        if (existing.isEmpty()) return null;
        if (!projectRepo.isProjectAdmin(requesterId, existing.get().getProjectId())) {
            throw new SecurityException("Only project admin can delete tasks");
        }
        TaskDto deletedTask = existing.get();
        boolean deleted = taskRepo.delete(taskId, requesterId);
        return deleted ? deletedTask : null;
    }

    public TaskDto updateTaskStatus(int taskId, int requesterId, String newStatus) throws SQLException {
        Optional<TaskDto> existing = taskRepo.findById(taskId);
        if (existing.isEmpty()) {
            return null;
        }

        if (!projectRepo.isProjectMember(requesterId, existing.get().getProjectId())) {
            throw new SecurityException("Access denied: you are not a member of this project");
        }

        boolean updated = taskRepo.updateStatus(taskId, newStatus);

        if (!updated) {
            return null;
        }

        return taskRepo.findById(taskId).orElse(null);
    }
}