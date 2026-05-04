package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Setter;

import java.net.http.HttpResponse;

public class DeleteUserPopupController {
    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private Label currentUserLabel;
    @FXML private Label confirmTextLabel;
    @FXML private Button deleteBtn;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Setter
    private MainController mainController;

    private UserDto user;
    private Runnable onDeleted;

    @FXML
    public void initialize() {
        playOpenAnimation();
    }

    public void setup(UserDto user, Runnable onDeleted) {
        this.user = user;
        this.onDeleted = onDeleted;
        currentUserLabel.setText(user != null && user.getNickname() != null ? user.getNickname() : "Selected user");
        confirmTextLabel.setText(user != null && user.getNickname() != null
                ? "Do you want to permanently delete user: " + user.getNickname()
                : "Do you want to permanently delete this user?");
    }

    @FXML
    private void handleDelete() {
        if (user == null) {
            closePopup();
            return;
        }

        deleteBtn.setDisable(true);
        deleteBtn.setText("Deleting...");

        new Thread(() -> {
            try {
                HttpResponse<String> response = apiClient.delete("/users/" + user.getId() + "/delete");
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(response)) {
                        if (onDeleted != null) {
                            onDeleted.run();
                        }
                        closePopup();
                    } else {
                        deleteBtn.setDisable(false);
                        deleteBtn.setText("Delete");
                        showAlert("Error", parseError(response.body()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    deleteBtn.setDisable(false);
                    deleteBtn.setText("Delete");
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String parseError(String responseBody) {
        try {
            java.util.Map<?, ?> errorMap = mapper.readValue(responseBody, java.util.Map.class);
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