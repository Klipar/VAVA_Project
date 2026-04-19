package com.rabbit.client.ui.controllers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.client.Config;

import java.net.URI;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ProjectsController {
    private final HttpClient client = HttpClient.newHttpClient();

    @FXML private FlowPane recentProjectsPane;
    @FXML private FlowPane yourProjectsPane;

    @FXML
    public void initialize() {
        List<ProjectDto> projects = getProjects();
    
        int recentCount = Math.min(3, projects.size());
        List<ProjectDto> recentProjects = projects.subList(0, recentCount);
        List<ProjectDto> yourProjects = projects;

        for (ProjectDto p : recentProjects) {
            recentProjectsPane.getChildren().add(createProjectCard(p));
        }
        for (ProjectDto p : yourProjects) {
            yourProjectsPane.getChildren().add(createProjectCard(p));
        }
        
        for (ProjectDto p : projects) {
            System.out.println("[Project] id=" + p.getId() + " title=" + p.getTitle());
        }

        UserDto user = Config.getInstance().getUser();
        if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.TEAM_LEADER) {
            Button addBtn = new Button("+");
            addBtn.setStyle("-fx-background-radius: 50; -fx-min-width: 50; -fx-min-height: 50; " +
                        "-fx-background-color: #4a9eda; -fx-text-fill: white; -fx-font-size: 24px; -fx-cursor: hand;");
            addBtn.setOnAction(e -> Config.getInstance().getMainController().loadView("create-project-view.fxml"));
            yourProjectsPane.getChildren().add(addBtn);
        }
    }

    private VBox createProjectCard(ProjectDto project) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: #0d2137; -fx-background-radius: 8; -fx-padding: 15;");

        Label title = new Label(project.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button openTasks = new Button("VIEW OPEN TASKS");
        openTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        Button assignedTasks = new Button("VIEW ASSIGNED TASKS");
        assignedTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        card.getChildren().addAll(title, openTasks, assignedTasks);

        if (project.getMasterId() == Config.getInstance().getUser().getId()) {
            Button deleteBtn = new Button("DELETE");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e05555; -fx-padding: 0; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> {
                deleteProject(project.getId());
                recentProjectsPane.getChildren().clear();
                yourProjectsPane.getChildren().clear();
                initialize();
            });
            card.getChildren().add(deleteBtn);
        }
        return card;
    }

    private List<ProjectDto> getProjects() {
        try {
            String token = Config.getInstance().getToken();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/projects"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            return mapper.readValue(response.body(), mapper.getTypeFactory().constructCollectionType(List.class, ProjectDto.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return List.of();
    }

    private void deleteProject(int projectId) {
        try {
            String token = Config.getInstance().getToken();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/projects/" + projectId))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[DeleteProject] status=" + response.statusCode());
            System.out.println("[DeleteProject] body=" + response.body());
            System.out.println("[DeleteProject] projectId=" + projectId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}