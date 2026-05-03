package com.rabbit.client.ui.controllers;

import com.rabbit.client.service.NotificationPollingService;
import com.rabbit.common.dto.NotificationDto;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class NotificationCenterController {
    @FXML private VBox rootContainer;
    @FXML private ListView<NotificationDto> notificationListView;
    @FXML private Label emptyStateLabel;
    @FXML private Button refreshButton;

    @Setter private MainController mainController;
    private final NotificationPollingService pollingService = NotificationPollingService.getInstance();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        setupListView();
        setupRefreshButton();
        loadNotifications();
    }

    private void setupListView() {
        notificationListView.setCellFactory(param -> new NotificationListCell());
        notificationListView.getStylesheets().add(
                getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm()
        );
    }

    private void setupRefreshButton() {
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> {
                pollingService.refreshNotifications();
                loadNotifications();
            });
        }
    }

    public void loadNotifications() {
        Platform.runLater(() -> {
            List<NotificationDto> notifications = pollingService.getCurrentNotifications();

            if (notifications == null || notifications.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                notificationListView.getItems().setAll(notifications);
            }
        });
    }

    private void showEmptyState() {
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setText("No notifications yet");
        }
        notificationListView.getItems().clear();
    }

    private void hideEmptyState() {
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(false);
        }
    }

    private class NotificationListCell extends ListCell<NotificationDto> {
        private final HBox container;
        private final Label messageLabel;
        private final Label dateLabel;
        private final Button markAsReadButton;
        private final VBox textContainer;

        NotificationListCell() {
            container = new HBox(10);
            container.setStyle("-fx-padding: 10; -fx-border-color: #1e3a5f; -fx-border-width: 0 0 1 0;");
            container.setAlignment(Pos.CENTER_LEFT);

            textContainer = new VBox(5);
            messageLabel = new Label();
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");

            dateLabel = new Label();
            dateLabel.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 11px;");

            textContainer.getChildren().addAll(messageLabel, dateLabel);
            VBox.setVgrow(messageLabel, Priority.ALWAYS);

            markAsReadButton = new Button("✓ Mark as read");
            markAsReadButton.setStyle(
                    "-fx-padding: 5 10; " +
                    "-fx-background-color: #1e90ff; " +
                    "-fx-text-fill: white; " +
                    "-fx-font-size: 11px; " +
                    "-fx-cursor: hand; " +
                    "-fx-border-radius: 3; " +
                    "-fx-background-radius: 3;"
            );
            markAsReadButton.setOnAction(event -> handleMarkAsRead());

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            container.getChildren().addAll(textContainer, spacer, markAsReadButton);
        }

        @Override
        protected void updateItem(NotificationDto notification, boolean empty) {
            super.updateItem(notification, empty);

            if (empty || notification == null) {
                setGraphic(null);
                return;
            }

            messageLabel.setText(notification.getMessage());

            if (notification.getCreated_at() != null) {
                dateLabel.setText("Sent: " + notification.getCreated_at().format(dateFormatter));
            }

            if (Boolean.TRUE.equals(notification.getIsRead())) {
                messageLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 13px;");
                container.setStyle("-fx-padding: 10; -fx-border-color: #1e3a5f; -fx-border-width: 0 0 1 0; -fx-opacity: 0.7;");
                markAsReadButton.setDisable(true);
                markAsReadButton.setStyle(
                        "-fx-padding: 5 10; " +
                        "-fx-background-color: #505050; " +
                        "-fx-text-fill: #808080; " +
                        "-fx-font-size: 11px; " +
                        "-fx-border-radius: 3; " +
                        "-fx-background-radius: 3;"
                );
            } else {
                messageLabel.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
                container.setStyle("-fx-padding: 10; -fx-border-color: #1e3a5f; -fx-border-width: 0 0 1 0; " +
                        "-fx-background-color: #0f2844;");
                markAsReadButton.setDisable(false);
            }

            setGraphic(container);
        }

        private void handleMarkAsRead() {
            NotificationDto notification = getItem();
            if (notification != null && notification.getId() != null) {
                pollingService.markAsRead(notification.getId());
                Platform.runLater(() -> {
                    loadNotifications();
                });
            }
        }
    }
}

