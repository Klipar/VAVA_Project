package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.UserDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class ProfileController {

    @FXML private TextField fullNameField;
    @FXML private TextField loginField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextArea skillsArea;
    @FXML private Circle avatarCircle;

    private final UserService userService = UserService.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        loadUserProfile();
        skillsArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                saveField("skills", skillsArea.getText());
            }
        });
    }

    private void loadUserProfile() {
        UserDto user = userService.getCurrentUser();
        if (user == null) return;
        fullNameField.setText(user.getName() != null ? user.getName() : "");
        loginField.setText(user.getNickname() != null ? user.getNickname() : "");
        emailField.setText(user.getEmail() != null ? user.getEmail() : "");
        skillsArea.setText(user.getSkills() != null ? user.getSkills() : "");
    }

    @FXML
    private void handleChangeName() {
        if (!fullNameField.isEditable()) {
            fullNameField.setEditable(true);
            fullNameField.requestFocus();
        } else {
            saveField("name", fullNameField.getText());
            fullNameField.setEditable(false);
        }
    }

    @FXML
    private void handleChangeEmail() {
        if (!emailField.isEditable()) {
            emailField.setEditable(true);
            emailField.requestFocus();
        } else {
            saveField("email", emailField.getText());
            emailField.setEditable(false);
        }
    }

    @FXML
    private void handleShowPassword() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password");
        alert.setHeaderText(null);
        alert.setContentText("Password is stored securely on the server and cannot be shown.");
        alert.showAndWait();
    }

    @FXML
    private void handleChangePassword() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Change Password");
        dialog.setHeaderText("Enter new password:");

        PasswordField pwField = new PasswordField();
        dialog.getDialogPane().setContent(pwField);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) return pwField.getText();
            return null;
        });

        dialog.showAndWait().ifPresent(newPw -> {
            if (!newPw.isBlank()) saveField("password", newPw);
        });
    }

    @FXML
    private void handleChangePhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select profile photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(avatarCircle.getScene().getWindow());
        if (file != null) {
            javafx.scene.image.Image img = new javafx.scene.image.Image(file.toURI().toString());
            avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
        }
    }

    @FXML
    private void handleExportSkills() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Skills");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.setInitialFileName("skills.txt");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*")
        );
        File file = chooser.showSaveDialog(skillsArea.getScene().getWindow());
        if (file == null) return;
        saveFieldSilent("skills", skillsArea.getText());
        try (PrintWriter pw = new PrintWriter(file)) {
            UserDto user = userService.getCurrentUser();
            pw.println("User: " + (user != null ? user.getNickname() : ""));
            pw.println("Name: " + (user != null ? user.getName() : ""));
            pw.println("\nSkills:\n" + skillsArea.getText());
            showAlert("Success", "Skills exported!");
        } catch (Exception e) {
            showAlert("Error", "Failed to export: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        try {
            userService.logout();
            Config.getInstance().setToken(null);
            Config.getInstance().setUser(null);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/rabbit/client/fxml/login_page.fxml")
            );
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(
                    getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm()
            );

            Stage stage = (Stage) fullNameField.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveField(String field, String value) {
        UserDto user = userService.getCurrentUser();
        if (user == null) return;

        new Thread(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put(field, value);
                String json = mapper.writeValueAsString(body);

                HttpResponse<String> response = apiClient.put("/users/" + user.getId() + "/update", json);

                if (apiClient.isSuccess(response)) {
                    UserDto updated = mapper.readValue(response.body(), UserDto.class);
                    Config.getInstance().setUser(updated);
                    userService.getCurrentSession().setUser(updated);
                    Platform.runLater(() -> showAlert("Success", field + " updated!"));
                } else {
                    Platform.runLater(() -> showAlert("Error", "Failed: " + response.body()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Error", e.getMessage()));
            }
        }).start();
    }

    private void saveFieldSilent(String field, String value) {
        UserDto user = userService.getCurrentUser();
        if (user == null) return;
        new Thread(() -> {
            try {
                Map<String, String> body = new HashMap<>();
                body.put(field, value);
                String json = mapper.writeValueAsString(body);
                HttpResponse<String> response = apiClient.put("/users/" + user.getId() + "/update", json);
                if (apiClient.isSuccess(response)) {
                    UserDto updated = mapper.readValue(response.body(), UserDto.class);
                    Config.getInstance().setUser(updated);
                    userService.getCurrentSession().setUser(updated);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public void refreshProfile() {
        loadUserProfile();
    }
}