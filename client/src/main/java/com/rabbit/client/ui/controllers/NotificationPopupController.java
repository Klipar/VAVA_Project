package com.rabbit.client.ui.controllers;

import com.rabbit.client.service.NotificationPollingService;
import com.rabbit.common.dto.NotificationDto;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class NotificationPopupController {
    @FXML private VBox rootContainer;
    @FXML private ScrollPane notificationsScrollPane;
    @FXML private VBox notificationsContainer;
    @FXML private Button closeButton;
    @FXML private Button lessButton;

    private final NotificationPollingService pollingService = NotificationPollingService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private Stage popupStage;
    private MainController mainController;

    @FXML
    public void initialize() {
        lessButton.setOnAction(event -> handleOpenNotificationCenter());
        loadNotifications();
    }

    public void setStage(Stage stage) {
        this.popupStage = stage;
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void loadNotifications() {
        List<NotificationDto> notifications = pollingService.getCurrentNotifications();
        notificationsContainer.getChildren().clear();

        if (notifications == null || notifications.isEmpty()) {
            Label emptyLabel = new Label("No notifications");
            emptyLabel.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 14px;");
            notificationsContainer.getChildren().add(emptyLabel);
        } else {
            int count = Math.min(5, notifications.size());
            for (int i = 0; i < count; i++) {
                NotificationDto notification = notifications.get(i);
                notificationsContainer.getChildren().add(createNotificationItem(notification));
            }
        }
    }

    private VBox createNotificationItem(NotificationDto notification) {
        VBox itemBox = new VBox(5);
        itemBox.setStyle(
                "-fx-background-color: #0f2844; " +
                "-fx-border-color: #1e3a5f; " +
                "-fx-border-width: 1; " +
                "-fx-border-radius: 5; " +
                "-fx-background-radius: 5; " +
                "-fx-padding: 12;"
        );

        Label messageLabel = new Label(notification.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-line-spacing: 2;");
        itemBox.getChildren().add(messageLabel);

        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label();
        if (notification.getCreated_at() != null) {
            timeLabel.setText(notification.getCreated_at().format(dateFormatter));
        }
        timeLabel.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (Boolean.FALSE.equals(notification.getIsRead())) {
            Label badgeLabel = new Label("New!");
            badgeLabel.setStyle(
                    "-fx-background-color: #ff6b6b;" +
                    "-fx-text-fill: white; " +
                    "-fx-padding: 2 8; " +
                    "-fx-font-size: 10px; " +
                    "-fx-border-radius: 10; " +
                    "-fx-background-radius: 10;"
            );
            bottomRow.getChildren().addAll(timeLabel, spacer, badgeLabel);
        } else {
            bottomRow.getChildren().addAll(timeLabel, spacer);
        }

        itemBox.getChildren().add(bottomRow);
        return itemBox;
    }

    @FXML
    private void handleClose() {
        if (popupStage != null) {
            TranslateTransition exitTransition = new TranslateTransition(Duration.millis(300), rootContainer);
            exitTransition.setByX(450);
            exitTransition.setOnFinished(event -> popupStage.close());
            exitTransition.play();
        }
    }

    private void handleOpenNotificationCenter() {
        handleClose();
        if (mainController != null) {
            mainController.loadView("notifications-view.fxml");
        }
    }

    public void animateIn() {
        if (rootContainer != null) {
            rootContainer.setTranslateX(450);
            TranslateTransition enterTransition = new TranslateTransition(Duration.millis(300), rootContainer);
            enterTransition.setToX(0);
            enterTransition.play();
        }
    }
}


