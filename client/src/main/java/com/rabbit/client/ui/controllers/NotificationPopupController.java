package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.client.service.NotificationPollingService;
import com.rabbit.common.dto.NotificationDto;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.ResourceBundle;

public class NotificationPopupController {

    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private VBox notificationsContainer;
    @FXML private Button refreshButton;
    @FXML private Button closeButton;
    @FXML private Label inboxLabel;
    @FXML private Label countLabel;

    private final NotificationPollingService pollingService = NotificationPollingService.getInstance();

    @FXML
    public void initialize() {
        ResourceBundle rb = Config.getInstance().getBundle();
        inboxLabel.setText(rb.getString("notif_inbox"));
        closeButton.setText(rb.getString("notif_close"));
        refreshButton.setText(rb.getString("notif_refresh"));
        refreshButton.setOnAction(e -> loadNotifications());
        loadNotifications();
        playOpenAnimation();
    }

    private void loadNotifications() {
        List<NotificationDto> notifications = pollingService.getCurrentNotifications();
        notificationsContainer.getChildren().clear();

        ResourceBundle rb = Config.getInstance().getBundle();
        if (notifications == null || notifications.isEmpty()) {
            Label empty = new Label(rb.getString("notif_no_notifications"));
            empty.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 14px;");
            notificationsContainer.getChildren().add(empty);
            countLabel.setText("0");
        } else {
            int unread = 0;
            for (NotificationDto n : notifications) {
                if (Boolean.FALSE.equals(n.getIsRead())) unread++;
                notificationsContainer.getChildren().add(buildItem(n));
            }
            countLabel.setText(String.format(rb.getString("notif_total"), notifications.size(), unread));
        }
    }

    private VBox buildItem(NotificationDto notification) {
        VBox box = new VBox(6);
        box.setStyle(
                "-fx-background-color: #112233; " +
                        "-fx-border-color: #1e3a5f; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 12;"
        );

        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");

        HBox bottom = new HBox(8);
        bottom.setAlignment(Pos.CENTER_LEFT);

        Label time = new Label();
        if (notification.getCreated_at() != null) {
            time.setText(notification.getCreated_at().format(Config.getInstance().getDateTimeFormatter()));
        }
        time.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 11px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ResourceBundle rb = Config.getInstance().getBundle();
        if (Boolean.FALSE.equals(notification.getIsRead())) {
            Button markReadBtn = new Button(rb.getString("notif_mark_read"));
            markReadBtn.setStyle(
                    "-fx-background-color: #1e90ff; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-size: 11px; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-radius: 3; " +
                            "-fx-padding: 3 8;"
            );
            markReadBtn.setOnAction(e -> {
                markReadBtn.setDisable(true);
                pollingService.markAsRead(notification.getId(), this::loadNotifications);
            });
            bottom.getChildren().addAll(time, spacer, markReadBtn);
        } else {
            Label readLabel = new Label(rb.getString("notif_read"));
            readLabel.setStyle("-fx-text-fill: #5a7a5a; -fx-font-size: 11px;");
            bottom.getChildren().addAll(time, spacer, readLabel);
        }

        box.getChildren().addAll(message, bottom);
        return box;
    }

    @FXML
    private void handleClose() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(150), popupCard);
        fade.setToValue(0);
        scale.setToX(0.9);
        scale.setToY(0.9);
        fade.setOnFinished(e -> {
            if (overlayPane.getParent() instanceof Pane parent) {
                parent.getChildren().remove(overlayPane);
            }
            NotificationPollingService.getInstance().onPopupClosed();
        });
        fade.play();
        scale.play();
    }

    @FXML
    private void handleOverlayClick(MouseEvent e) {
        if (e.getTarget() == overlayPane) {
            handleClose();
        }
    }

    @FXML
    private void consumeClick(MouseEvent e) {
        e.consume();
    }

    private void playOpenAnimation() {
        overlayPane.setOpacity(0);
        popupCard.setScaleX(0.9);
        popupCard.setScaleY(0.9);
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), popupCard);
        fade.setToValue(1);
        scale.setToX(1);
        scale.setToY(1);
        fade.play();
        scale.play();
    }
}