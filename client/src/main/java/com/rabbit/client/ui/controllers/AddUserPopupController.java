package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.enums.UserRole;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
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

public class AddUserPopupController {
    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private TextField loginField;
    @FXML private PasswordField passwordField;
    @FXML private RadioButton workerRoleRadio;
    @FXML private RadioButton managerRoleRadio;
    @FXML private RadioButton teamLeaderRoleRadio;
    @FXML private Button createBtn;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final ToggleGroup roleGroup = new ToggleGroup();

    @Setter
    private MainController mainController;

    private Runnable onCreated;

    @FXML
    public void initialize() {
        workerRoleRadio.setToggleGroup(roleGroup);
        managerRoleRadio.setToggleGroup(roleGroup);
        teamLeaderRoleRadio.setToggleGroup(roleGroup);
        workerRoleRadio.setSelected(true);
        playOpenAnimation();
    }

    public void setup(Runnable onCreated) {
        this.onCreated = onCreated;
    }

    @FXML
    private void handleCreate() {
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if (login.isBlank() || password.isBlank()) {
            showAlert("Validation Error", "Please enter both login and password");
            return;
        }

        createBtn.setDisable(true);
        createBtn.setText("Creating...");

        new Thread(() -> {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("nickname", login);
                requestBody.put("email", login + "@example.com");
                requestBody.put("password", password);
                requestBody.put("name", login);
                requestBody.put("role", resolveRequestedRole().name());

                HttpResponse<String> response = apiClient.post("/users/create", mapper.writeValueAsString(requestBody));

                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        
                        if (onCreated != null) {
                            onCreated.run();
                        }
                        closePopup();
                    } else {
                        createBtn.setDisable(false);
                        createBtn.setText("Create user");
                        showAlert("Error", parseError(response.body()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    createBtn.setText("Create user");
                    showAlert("Error", e.getMessage());
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

    private UserRole resolveRequestedRole() {
        if (teamLeaderRoleRadio.isSelected()) {
            return UserRole.TEAM_LEADER;
        } else if (managerRoleRadio.isSelected()) {
            return UserRole.MANAGER;
        } else {
            return UserRole.WORKER;
        }
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
            return error != null ? error.toString() : "Unknown error";
        } catch (Exception e) {
            return "Unknown error";
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
}