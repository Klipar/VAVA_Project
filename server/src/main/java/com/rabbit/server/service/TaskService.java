package com.rabbit.server.service;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.server.repository.TaskRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class TaskService {
    private final TaskRepository repo = new TaskRepository();

    public List<TaskDto> getTasksByProjectId(int projectId) throws SQLException {
        return repo.findByProjectId(projectId);
    }

    public void createTask(int projectId, int requesterId, TaskDto dto) throws SQLException {
        if (!repo.isProjectAdmin(requesterId, projectId)) {
            throw new SecurityException("Only project admin can create tasks");
        }
        dto.setProjectId(projectId);
        dto.setCreatedBy(requesterId);
        repo.create(dto);
    }

    public boolean updateTask(int taskId, int requesterId, TaskDto dto) throws SQLException {
        Optional<TaskDto> existing = repo.findById(taskId);
        if(existing.isEmpty()) return false;
        if(!repo.isProjectAdmin(requesterId, existing.get().getProjectId())) {
            throw new SecurityException("Only project admin can update tasks");
        }
        dto.setId(taskId);
        return repo.update(dto);
    }

    public boolean deleteTask(int taskId, int requesterId) throws SQLException{
        Optional<TaskDto> existing = repo.findById(taskId);
        if(existing.isEmpty()) return false;
        if(!repo.isProjectAdmin(requesterId, existing.get().getProjectId())) {
            throw new SecurityException("Only project admin can delete tasks");
        }
        return repo.delete(taskId);
    }
}
