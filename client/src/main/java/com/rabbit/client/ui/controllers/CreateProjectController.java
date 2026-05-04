package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.ProjectStatus;
import com.rabbit.common.enums.UserRole;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CreateProjectController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField assignField;
    @FXML private FlowPane assignedChipsPane;
    @FXML private DatePicker deadlinePicker;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<String> assignedPeople = new ArrayList<>();
    private final List<UserDto> assignedUsers = new ArrayList<>();

    @FXML
    private void handleAddAssign() {
        String name = assignField.getText().trim();
        if (name.isBlank()) return;

        boolean alreadyAdded = assignedUsers.stream()
                .anyMatch(u -> u.getNickname() != null && u.getNickname().equalsIgnoreCase(name));
        java.util.ResourceBundle rb = Config.getInstance().getBundle();
        if (alreadyAdded) {
            showAlert(rb.getString("user_already_assigned") + ": " + name);
            return;
        }

        UserDto currentUser = Config.getInstance().getUser();
        if (currentUser.getNickname() != null && currentUser.getNickname().equalsIgnoreCase(name)) {
            showAlert(rb.getString("creator_auto_added"));
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:6969/users/nickname/" + name))
                .header("Authorization", "Bearer " + Config.getInstance().getToken())
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                UserDto fetchedUser = mapper.readValue(response.body(), UserDto.class);

                assignedPeople.add(name);
                assignedUsers.add(fetchedUser);
                assignField.clear();

                // Create chip
                HBox chip = new HBox(5);
                chip.setStyle("-fx-background-color: #1a3a5c; -fx-background-radius: 20; -fx-padding: 5 10 5 10;");
                Label nameLabel = new Label(name);
                nameLabel.setStyle("-fx-text-fill: white;");
                Button removeBtn = new Button("x");
                removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 0;");
                removeBtn.setOnAction(e -> {
                    assignedPeople.remove(name);
                    assignedUsers.removeIf(u -> u.getId() != null && fetchedUser.getId() != null
                            && u.getId().longValue() == fetchedUser.getId().longValue());
                    assignedChipsPane.getChildren().remove(chip);
                });
                chip.getChildren().addAll(nameLabel, removeBtn);
                assignedChipsPane.getChildren().add(chip);
            } else {
                showAlert(rb.getString("user_not_found") + ": " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(rb.getString("error_fetch_user"));
        }

    }

    private boolean handleAddingAssigned(UserDto user, int projectId) {
        try {
                String token = Config.getInstance().getToken();
                if (token == null || token.isBlank()) {
                    showAlert(Config.getInstance().getBundle().getString("not_logged_in"));
                    return false;
                }
                
                //POST /projects/{projectId}/users/{userId}/add
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:6969/projects/" + projectId + "/users/" + user.getId() + "/add"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token.trim())
                        .POST(HttpRequest.BodyPublishers.ofString("{}"))
                        .build();
    
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
                int status = response.statusCode();
                String responseBody = response.body();
    
                // Debug logs for terminal
                System.out.println("[FetchUser] status=" + status);
                System.out.println("[FetchUser] body=" + responseBody);
    
                if (status == 200 || status == 201) {
                    return true;
                } else if (status == 401) {
                    showAlert("Unauthorized (401). Your session is invalid or expired. Please login again.");
                } else if (status == 403) {
                    showAlert("Forbidden (403). You are authenticated but do not have permission.");
                } else {
                    showAlert("Failed to fetch user (" + status + "): " + responseBody);
                }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to parse user data for: " + user.getNickname());
        }
        return false;
    }

    @FXML
    private void handleCreateProject() {
        UserDto userTest = Config.getInstance().getUser();
        java.util.ResourceBundle rb = Config.getInstance().getBundle();
        if (!(userTest.getRole() == UserRole.MANAGER || userTest.getRole() == UserRole.TEAM_LEADER)) {
            showAlert(rb.getString("no_permission_project"));
            return;
        }

        String name = nameField.getText().trim();
        if (name.isBlank()) {
            showAlert(rb.getString("enter_project_name"));
            return;
        }

        LocalDate deadlineDate = deadlinePicker.getValue();
        if (deadlineDate == null) {
            showAlert(rb.getString("select_deadline_msg"));
            return;
        }

        try {
            String token = Config.getInstance().getToken();
            if (token == null || token.isBlank()) {
                showAlert(rb.getString("not_logged_in"));
                return;
            }

            ProjectDto dto = new ProjectDto();
            dto.setTitle(name);
            dto.setDescription(descriptionField.getText().trim());
            dto.setStatus(ProjectStatus.ACTIVE);
            dto.setDeadline(deadlineDate.atStartOfDay());

            String body = mapper.writeValueAsString(dto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/projects"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            int status = response.statusCode();
            String responseBody = response.body();

            // Debug logs for terminal
            System.out.println("[CreateProject] status=" + status);
            System.out.println("[CreateProject] body=" + responseBody);

            if (status == 201) {
                int projectId = mapper.readTree(responseBody).get("id").asInt();

                for (UserDto user : new ArrayList<>(assignedUsers)) {
                    handleAddingAssigned(user, projectId);
                }

                showAlert(rb.getString("project_created"));
                Config.getInstance().getMainController().loadView("projects-view.fxml");
                return;
            } else if (status == 401) {
                showAlert(rb.getString("unauthorized_msg"));
            } else if (status == 403) {
                showAlert(rb.getString("forbidden_msg"));
            } else {
                showAlert(rb.getString("update_failed") + " (" + status + "): " + responseBody);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(rb.getString("connection_error"));
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }
}