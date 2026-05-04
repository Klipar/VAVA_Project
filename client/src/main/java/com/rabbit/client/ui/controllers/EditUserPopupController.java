package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EditUserPopupController {
    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private Label currentUserLabel;
    @FXML private TextField loginField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField skillsField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton workerRoleRadio;
    @FXML private RadioButton managerRoleRadio;
    @FXML private RadioButton teamLeaderRoleRadio;
    @FXML private Button saveBtn;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Setter
    private MainController mainController;

    private UserDto user;
    private Consumer<UserDto> onSaved;

    @FXML
    public void initialize() {
        var rb = Config.getInstance().getBundle();
        workerRoleRadio.setText(rb.getString("add_user_role_worker"));
        teamLeaderRoleRadio.setText(rb.getString("add_user_role_team_leader"));
        managerRoleRadio.setText(rb.getString("add_user_role_manager"));
        saveBtn.setText(rb.getString("edit_user_save"));
        playOpenAnimation();
        ToggleGroup roleGroup = new ToggleGroup();
        workerRoleRadio.setToggleGroup(roleGroup);
        managerRoleRadio.setToggleGroup(roleGroup);
        teamLeaderRoleRadio.setToggleGroup(roleGroup);
    }

    public void setup(UserDto user, Consumer<UserDto> onSaved) {
        this.user = user;
        this.onSaved = onSaved;
        currentUserLabel.setText(user != null && user.getNickname() != null
                ? user.getNickname() : Config.getInstance().getBundle().getString("edit_user_selected"));
        loginField.setText(user != null && user.getNickname() != null ? user.getNickname() : "");
        fullNameField.setText(user != null && user.getName() != null ? user.getName() : "");
        emailField.setText(user != null && user.getEmail() != null ? user.getEmail() : "");
        skillsField.setText(user != null && user.getSkills() != null ? user.getSkills() : "");
        workerRoleRadio.setSelected(user != null && user.getRole() == UserRole.WORKER);
        managerRoleRadio.setSelected(user != null && user.getRole() == UserRole.MANAGER);
        teamLeaderRoleRadio.setSelected(user != null && user.getRole() == UserRole.TEAM_LEADER);
    }

    @FXML
    private void handleSave() {
        if (user == null) {
            closePopup();
            return;
        }

        String nickname = loginField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String skills = skillsField.getText().trim();
        String password = passwordField.getText().trim();
        UserRole role = getSelectedRole();

        if (nickname.isBlank() || fullName.isBlank() || email.isBlank()) {
            var rb = Config.getInstance().getBundle();
            showAlert(rb.getString("validation_error"), rb.getString("edit_user_validation"));
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        new Thread(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("nickname", nickname);
                requestBody.put("name", fullName);
                requestBody.put("email", email);
                requestBody.put("role", role);
                if (!skills.isBlank()) {
                    requestBody.put("skills", skills);
                }
                if (!password.isBlank()) {
                    requestBody.put("password", password);
                }

                HttpResponse<String> response = apiClient.put("/users/" + user.getId() + "/update", mapper.writeValueAsString(requestBody));

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        try {
                            UserDto updated = mapper.readValue(response.body(), UserDto.class);
                            if (onSaved != null) {
                                onSaved.accept(updated);
                            }
                            closePopup();
                        } catch (Exception e) {
                            if (onSaved != null) {
                                onSaved.accept(user);
                            }
                            closePopup();
                        }
                    } else {
                        saveBtn.setDisable(false);
                        saveBtn.setText(Config.getInstance().getBundle().getString("edit_user_save"));
                        showAlert(Config.getInstance().getBundle().getString("error"), parseError(response.body()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText(Config.getInstance().getBundle().getString("edit_user_save"));
                    showAlert(Config.getInstance().getBundle().getString("error"), e.getMessage());
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        closePopup();
    }

    @FXML
    private void handleOverlayClick(MouseEvent event) {
        if (event.getTarget() == overlayPane) {
            closePopup();
        }
    }

    @FXML
    private void consumeClick(MouseEvent event) {
        event.consume();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String parseError(String responseBody) {
        try {
            Map<?, ?> errorMap = mapper.readValue(responseBody, Map.class);
            Object error = errorMap.get("error");
            return error != null ? error.toString() : Config.getInstance().getBundle().getString("add_user_unknown_error");
        } catch (Exception e) {
            return Config.getInstance().getBundle().getString("add_user_unknown_error");
        }
    }

    private void closePopup() {
        FadeTransition fade = new FadeTransition(Duration.millis(160), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(160), popupCard);
        fade.setToValue(0);
        scale.setToX(0.85);
        scale.setToY(0.85);
        fade.setOnFinished(e -> {
            if (overlayPane.getParent() instanceof Pane parent) {
                overlayPane.prefWidthProperty().unbind();
                overlayPane.prefHeightProperty().unbind();
                parent.getChildren().remove(overlayPane);
            } else if (overlayPane.getScene() != null && overlayPane.getScene().getWindow() instanceof Stage stage) {
                stage.close();
            }
        });
        fade.play();
        scale.play();
    }

    private void playOpenAnimation() {
        overlayPane.setOpacity(0);
        popupCard.setScaleX(0.85);
        popupCard.setScaleY(0.85);
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(220), popupCard);
        fade.setToValue(1);
        scale.setToX(1);
        scale.setToY(1);
        fade.play();
        scale.play();
    }

    private UserRole getSelectedRole() {
        if (teamLeaderRoleRadio.isSelected()) {
            return UserRole.TEAM_LEADER;
        } else if (managerRoleRadio.isSelected()) {
            return UserRole.MANAGER;
        } else {
            return UserRole.WORKER;
        }
    }
}