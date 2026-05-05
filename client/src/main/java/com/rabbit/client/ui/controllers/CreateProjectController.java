package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.ProjectStatus;
import com.rabbit.common.enums.UserRole;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CreateProjectController {
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField assignField;
    @FXML private FlowPane assignedChipsPane;
    @FXML private DatePicker deadlinePicker;
    @FXML private ListView<String> suggestionsListView;
    @FXML private StackPane suggestionsPane;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<String> assignedPeople = new ArrayList<>();
    private final List<UserDto> assignedUsers = new ArrayList<>();
    private List<UserDto> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        setupSuggestionsListView();
        setupAssignFieldListener();
        loadAllUsers();
    }

    private void setupSuggestionsListView() {
        suggestionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });

        suggestionsListView.setOnMouseClicked(event -> {
            String selected = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                assignField.setText(selected);
                hideSuggestions();
                addUserByNickname(selected);
            }
        });
    }

     private void setupAssignFieldListener() {
        assignField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                hideSuggestions();
                return;
            }
            String query = newVal.trim().toLowerCase();
            List<String> matches = allUsers.stream()
                .filter(u -> u.getNickname() != null &&
                             u.getNickname().toLowerCase().contains(query) &&
                             !isAlreadyAssigned(u))
                .map(UserDto::getNickname)
                .limit(8)
                .collect(Collectors.toList());

            if (matches.isEmpty()) {
                hideSuggestions();
            } else {
                suggestionsListView.getItems().setAll(matches);
                // Adjust height: 36px per item, max 180
                suggestionsListView.setPrefHeight(Math.min(matches.size() * 36, 180));
                showSuggestions();
            }
        });

        // Hide on focus lost
        assignField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                // Slight delay so click on list item can register first
                new Thread(() -> {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                    Platform.runLater(this::hideSuggestions);
                }).start();
            }
        });
    }

    private void showSuggestions() {
        suggestionsListView.setVisible(true);
        suggestionsListView.setManaged(true);
    }

    private void hideSuggestions() {
        suggestionsListView.setVisible(false);
        suggestionsListView.setManaged(false);
        suggestionsListView.getSelectionModel().clearSelection();
    }

     private boolean isAlreadyAssigned(UserDto user) {
        return assignedUsers.stream().anyMatch(u ->
            u.getId() != null && user.getId() != null &&
            u.getId().longValue() == user.getId().longValue()
        );
    }

    /** Fetch all users once so autocomplete works offline/fast. */
    private void loadAllUsers() {
        new Thread(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/users/all"))
                    .header("Authorization", "Bearer " + Config.getInstance().getToken())
                    .GET()
                    .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    List<UserDto> users = mapper.readValue(response.body(), new TypeReference<>() {});
                    Platform.runLater(() -> allUsers = users);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addUserByNickname(String name) {
        java.util.ResourceBundle rb = Config.getInstance().getBundle();

        boolean alreadyAdded = assignedUsers.stream()
            .anyMatch(u -> u.getNickname() != null && u.getNickname().equalsIgnoreCase(name));
        if (alreadyAdded) {
            showAlert(rb.getString("user_already_assigned") + ": " + name);
            return;
        }

        UserDto currentUser = Config.getInstance().getUser();
        if (currentUser.getNickname() != null && currentUser.getNickname().equalsIgnoreCase(name)) {
            showAlert(rb.getString("creator_auto_added"));
            return;
        }

        // Try to resolve from already-loaded list first (fast path)
        UserDto cached = allUsers.stream()
            .filter(u -> u.getNickname() != null && u.getNickname().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);

        if (cached != null) {
            addChip(cached);
            assignField.clear();
            return;
        }

        // Fallback: fetch from server
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:6969/users/nickname/" + name))
            .header("Authorization", "Bearer " + Config.getInstance().getToken())
            .GET()
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                UserDto fetchedUser = mapper.readValue(response.body(), UserDto.class);
                addChip(fetchedUser);
                assignField.clear();
            } else {
                showAlert(rb.getString("user_not_found") + ": " + name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(rb.getString("error_fetch_user"));
        }
    }

    private void addChip(UserDto user) {
        assignedPeople.add(user.getNickname());
        assignedUsers.add(user);

        HBox chip = new HBox(5);
        chip.getStyleClass().add("assign-chip");
        Label nameLabel = new Label(user.getNickname());
        nameLabel.getStyleClass().add("assign-chip-label");
        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("assign-chip-remove");
        removeBtn.setOnAction(e -> {
            assignedPeople.remove(user.getNickname());
            assignedUsers.removeIf(u ->
                u.getId() != null && user.getId() != null &&
                u.getId().longValue() == user.getId().longValue()
            );
            assignedChipsPane.getChildren().remove(chip);
        });
        chip.getChildren().addAll(nameLabel, removeBtn);
        assignedChipsPane.getChildren().add(chip);
    }

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