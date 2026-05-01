package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Setter;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AdminController {

    // Add user section
    @FXML private TextField addLoginField;
    @FXML private PasswordField addPasswordField;
    @FXML private Button createUserButton;

    // Edit user section
    @FXML private TextField searchLoginField;
    @FXML private Button searchButton;
    @FXML private TextField editLoginField;
    @FXML private TextField editEmailField;
    @FXML private TextField editFullNameField;
    @FXML private PasswordField editPasswordField;
    @FXML private Button deleteUserButton;
    @FXML private Button saveChangesButton;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final UserService userService = UserService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Setter
    private MainController mainController;

    private UserDto currentEditingUser;

    @FXML
    public void initialize() {
        setupButtonActions();
    }

    private void setupButtonActions() {
        createUserButton.setOnAction(event -> handleCreateUser());
        searchButton.setOnAction(event -> handleSearchUser());
        deleteUserButton.setOnAction(event -> handleDeleteUser());
        saveChangesButton.setOnAction(event -> handleSaveChanges());
    }

    private void handleCreateUser() {
        String login = addLoginField.getText().trim();
        String password = addPasswordField.getText().trim();

        if (login.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Please enter both login and password");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AdminController] Creating user with login: " + login);
                
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("nickname", login);
                requestBody.put("email", login + "@example.com");
                requestBody.put("password", password);
                requestBody.put("name", login);
                requestBody.put("role", "WORKER");

                String jsonBody = mapper.writeValueAsString(requestBody);
                System.out.println("[AdminController] Request body: " + jsonBody);
                String path = "/users/create";
                System.out.println("[AdminController] POST " + path);
                HttpResponse<String> response = apiClient.post(path, jsonBody);

                System.out.println("[AdminController] Response status: " + response.statusCode());
                System.out.println("[AdminController] Response body: " + response.body());

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        System.out.println("[AdminController] User created successfully: " + login);
                        showAlert("Success", "User created successfully");
                        addLoginField.clear();
                        addPasswordField.clear();
                    } else {
                        String errorMsg = parseErrorMessage(response.body());
                        System.out.println("[AdminController] Failed to create user: " + errorMsg);
                        showAlert("Error", "Failed to create user: " + errorMsg);
                    }
                });
            } catch (Exception e) {
                System.out.println("[AdminController] Exception during user creation: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to create user: " + e.getMessage()));
            }
        }).start();
    }

    private void handleSearchUser() {
        String login = searchLoginField.getText().trim();

        if (login.isEmpty()) {
            showAlert("Validation Error", "Please enter a login to search");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AdminController] Searching for user with login: " + login);
                HttpResponse<String> response = apiClient.get("/users/nickname/" + login);

                System.out.println("[AdminController] Search response status: " + response.statusCode());
                System.out.println("[AdminController] Search response body: " + response.body());

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        try {
                            currentEditingUser = mapper.readValue(response.body(), UserDto.class);
                            System.out.println("[AdminController] User found: " + currentEditingUser.getNickname() + 
                                             " (ID: " + currentEditingUser.getId() + ")");
                            loadUserToEditForm();
                        } catch (Exception e) {
                            System.out.println("[AdminController] Failed to parse user data: " + e.getMessage());
                            showAlert("Error", "Failed to parse user data: " + e.getMessage());
                        }
                    } else {
                        System.out.println("[AdminController] User not found: " + login);
                        showAlert("Not Found", "User with login '" + login + "' not found");
                    }
                });
            } catch (Exception e) {
                System.out.println("[AdminController] Exception during user search: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to search user: " + e.getMessage()));
            }
        }).start();
    }

    private void loadUserToEditForm() {
        if (currentEditingUser == null) return;

        editLoginField.setText(currentEditingUser.getNickname() != null ? currentEditingUser.getNickname() : "");
        editEmailField.setText(currentEditingUser.getEmail() != null ? currentEditingUser.getEmail() : "");
        editFullNameField.setText(currentEditingUser.getName() != null ? currentEditingUser.getName() : "");
        editPasswordField.clear();
    }

    private void handleDeleteUser() {
        if (currentEditingUser == null) {
            showAlert("Error", "Please search for a user first");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete User");
        confirmation.setContentText("Are you sure you want to delete user '" + currentEditingUser.getNickname() + "'?");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            System.out.println("[AdminController] Delete operation cancelled by user");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AdminController] Deleting user: " + currentEditingUser.getNickname() +
                        " (ID: " + currentEditingUser.getId() + ")");
                HttpResponse<String> response = apiClient.delete("/users/" + currentEditingUser.getId() + "/delete");

                System.out.println("[AdminController] Delete response status: " + response.statusCode());
                System.out.println("[AdminController] Delete response body: " + response.body());

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        System.out.println("[AdminController] User deleted successfully: " + currentEditingUser.getNickname());
                        showAlert("Success", "User deleted successfully");
                        clearEditForm();
                    } else {
                        String errorMsg = parseErrorMessage(response.body());
                        System.out.println("[AdminController] Failed to delete user: " + errorMsg);
                        showAlert("Error", "Failed to delete user: " + errorMsg);
                    }
                });
            } catch (Exception e) {
                System.out.println("[AdminController] Exception during user deletion: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to delete user: " + e.getMessage()));
            }
        }).start();
    }

    private void handleSaveChanges() {
        if (currentEditingUser == null) {
            showAlert("Error", "Please search for a user first");
            return;
        }

        String email = editEmailField.getText().trim();
        String fullName = editFullNameField.getText().trim();
        String password = editPasswordField.getText().trim();

        if (email.isEmpty() || fullName.isEmpty()) {
            showAlert("Validation Error", "Email and Full Name are required");
            return;
        }

        new Thread(() -> {
            try {
                System.out.println("[AdminController] Updating user: " + currentEditingUser.getNickname() +
                        " (ID: " + currentEditingUser.getId() + ")");
                System.out.println("[AdminController] New email: " + email + ", Full name: " + fullName);

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("email", email);
                requestBody.put("name", fullName);

                if (!password.isEmpty()) {
                    requestBody.put("password", password);
                    System.out.println("[AdminController] Password will be updated");
                }

                String jsonBody = mapper.writeValueAsString(requestBody);
                System.out.println("[AdminController] Update request body: " + jsonBody);
                HttpResponse<String> response = apiClient.put("/users/" + currentEditingUser.getId() + "/update", jsonBody);

                System.out.println("[AdminController] Update response status: " + response.statusCode());
                System.out.println("[AdminController] Update response body: " + response.body());

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        System.out.println("[AdminController] User updated successfully: " + currentEditingUser.getNickname());
                        showAlert("Success", "User updated successfully");
                        try {
                            currentEditingUser = mapper.readValue(response.body(), UserDto.class);
                            loadUserToEditForm();
                        } catch (Exception e) {
                            // Ignore parsing errors on success
                            System.out.println("[AdminController] Could not parse updated user data: " + e.getMessage());
                        }
                    } else {
                        String errorMsg = parseErrorMessage(response.body());
                        System.out.println("[AdminController] Failed to update user: " + errorMsg);
                        showAlert("Error", "Failed to update user: " + errorMsg);
                    }
                });
            } catch (Exception e) {
                System.out.println("[AdminController] Exception during user update: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Error", "Failed to update user: " + e.getMessage()));
            }
        }).start();
    }

    private void clearEditForm() {
        searchLoginField.clear();
        editLoginField.clear();
        editEmailField.clear();
        editFullNameField.clear();
        editPasswordField.clear();
        currentEditingUser = null;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String parseErrorMessage(String responseBody) {
        try {
            Map<String, Object> errorMap = mapper.readValue(responseBody, Map.class);
            Object error = errorMap.get("error");
            if (error != null) {
                return error.toString();
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return "Unknown error";
    }
}
