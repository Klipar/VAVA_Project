package com.rabbit.client.ui.controllers;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.client.Config;

import java.net.URI;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
        card.setPrefSize(240, 140);
        card.setCursor(javafx.scene.Cursor.HAND);
        card.setOnMouseClicked(e -> navigateToProjectBoard(project));

        card.setStyle("-fx-background-color: #0d2137; -fx-background-radius: 8; -fx-padding: 15;");

        Label title = new Label(project.getTitle());
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");


        java.util.ResourceBundle rb = Config.getInstance().getBundle();
        Button openTasks = new Button(rb.getString("view_open_tasks"));
        openTasks.setOnAction(e -> navigateToProjectTasks(project));
        openTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        UserDto user = Config.getInstance().getUser();
        Button assignedTasks = new Button();
        if (user.getRole() != UserRole.MANAGER) {
            assignedTasks = new Button(rb.getString("view_assigned_tasks"));
            assignedTasks.setOnAction(e -> navigateToAssignedTasks(project));
        }

        assignedTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        card.getChildren().addAll(title, openTasks, assignedTasks);

        if (project.getMasterId() == Config.getInstance().getUser().getId()) {
            Button deleteBtn = new Button(rb.getString("delete"));
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e05555; -fx-padding: 0; -fx-cursor: hand;");
            deleteBtn.setOnAction(e -> {
                if (confirmProjectDeletion(project.getTitle())) {
                    deleteProject(project.getId());
                    recentProjectsPane.getChildren().clear();
                    yourProjectsPane.getChildren().clear();
                    initialize();
                }
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

    private boolean confirmProjectDeletion(String projectTitle) {
        java.util.ResourceBundle rb = Config.getInstance().getBundle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(rb.getString("delete_project_title"));
        alert.setHeaderText(rb.getString("delete_project_header") + " \"" + projectTitle + "\"");
        alert.setContentText(rb.getString("delete_project_confirm"));

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void navigateToProjectBoard(ProjectDto project) {
        Config.getInstance().getMainController().loadView("board-page.fxml", project.getId(), project.getTitle());
    }

    private void navigateToAssignedTasks(ProjectDto project) {
        Config.getInstance().getMainController().loadView("my-tasks-view.fxml", project.getId(), project.getTitle());
    }

    private void navigateToProjectTasks(ProjectDto project) {
        Config.getInstance().getMainController().loadView("project-tasks-view.fxml", project.getId(), project.getTitle());
    }
}