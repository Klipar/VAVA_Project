package com.rabbit.client.service;

import com.rabbit.client.ui.controllers.MainController;
import com.rabbit.client.ui.controllers.NotificationPopupController;
import com.rabbit.common.dto.NotificationDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NotificationPollingService {
    private static NotificationPollingService instance;
    private final ApiClient apiClient;
    private final ScheduledExecutorService executor;
    private final Set<Long> shownNotificationIds;
    private boolean isRunning;
    private List<NotificationDto> currentNotifications;
    private Stage notificationPopupStage;

    private static final long POLL_INTERVAL_SECONDS = 60;
    private static final String INFO_COLOR = "#1e90ff";
    private static final String SUCCESS_COLOR = "#32cd32";
    private static final String WARNING_COLOR = "#ffa500";

    private NotificationPollingService() {
        this.apiClient = ApiClient.getInstance();
        this.executor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "NotificationPollingThread");
            t.setDaemon(true);
            return t;
        });
        this.shownNotificationIds = Collections.synchronizedSet(new HashSet<>());
        this.isRunning = false;
        this.currentNotifications = new ArrayList<>();
    }

    public static synchronized NotificationPollingService getInstance() {
        if (instance == null) {
            instance = new NotificationPollingService();
        }
        return instance;
    }

    public void start() {
        if (isRunning) {
            return;
        }
        isRunning = true;
        log.info("Notification polling service started");


        pollNotifications();


        executor.scheduleAtFixedRate(
            this::pollNotifications,
            POLL_INTERVAL_SECONDS,
            POLL_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    public void stop() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification polling service stopped");
    }

    private void pollNotifications() {
        try {
            List<NotificationDto> notifications = apiClient.getNotifications();

            Platform.runLater(() -> {
                if (notifications != null) {
                    for (NotificationDto notification : notifications) {
                        if (notification.getId() != null &&
                            !shownNotificationIds.contains(notification.getId()) &&
                            Boolean.FALSE.equals(notification.getIsRead())) {

                            MainController controller = MainController.getInstance();
                            if (controller != null) {
                                String color = determineNotificationColor(notification.getMessage());
                                controller.showGlobalNotification(notification.getMessage(), color);
                            }

                            shownNotificationIds.add(notification.getId());
                        }
                    }

                    currentNotifications = new ArrayList<>(notifications);
                }
            });
        } catch (Exception e) {
            log.warn("Error polling notifications: {}", e.getMessage());
        }
    }

    private String determineNotificationColor(String message) {
        if (message.toLowerCase().contains("error") || message.toLowerCase().contains("failed")) {
            return "#ff6347"; // Tomato red
        } else if (message.toLowerCase().contains("success") || message.toLowerCase().contains("completed")) {
            return SUCCESS_COLOR;
        } else if (message.toLowerCase().contains("warning") || message.toLowerCase().contains("deadline")) {
            return WARNING_COLOR;
        }
        return INFO_COLOR;
    }

    public List<NotificationDto> getCurrentNotifications() {
        return new ArrayList<>(currentNotifications);
    }

    public void markAsRead(long notificationId) {
        executor.execute(() -> {
            try {
                apiClient.markNotificationAsRead(notificationId);
                currentNotifications = currentNotifications.stream()
                    .peek(n -> {
                        if (n.getId() != null && n.getId().equals(notificationId)) {
                            n.setIsRead(true);
                        }
                    })
                    .toList();
                log.info("Notification {} marked as read", notificationId);
            } catch (Exception e) {
                log.warn("Error marking notification as read: {}", e.getMessage());
            }
        });
    }

    public void refreshNotifications() {
        pollNotifications();
    }

    public void showNotificationPopup() {
        Platform.runLater(() -> {
            try {
                if (notificationPopupStage != null && notificationPopupStage.isShowing()) {
                    notificationPopupStage.toFront();
                    return;
                }

                FXMLLoader loader = new FXMLLoader(
                    NotificationPollingService.class.getResource("/com/rabbit/client/fxml/notification-popup.fxml")
                );
                Scene scene = new Scene(loader.load());
                scene.setFill(javafx.scene.paint.Color.TRANSPARENT);

                notificationPopupStage = new Stage();
                notificationPopupStage.setScene(scene);
                notificationPopupStage.setTitle("Notifications");
                notificationPopupStage.initStyle(StageStyle.TRANSPARENT);
                notificationPopupStage.setWidth(420);
                notificationPopupStage.setHeight(600);

                NotificationPopupController controller = loader.getController();
                controller.setStage(notificationPopupStage);
                controller.setMainController(MainController.getInstance());

                double screenWidth = javafx.stage.Screen.getPrimary().getVisualBounds().getWidth();
                double screenHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight();
                notificationPopupStage.setX(screenWidth - 430);
                notificationPopupStage.setY(screenHeight - 650);

                notificationPopupStage.show();
                controller.animateIn();

            } catch (Exception e) {
                log.error("Error showing notification popup: {}", e.getMessage());
            }
        });
    }

    public void closeNotificationPopup() {
        if (notificationPopupStage != null && notificationPopupStage.isShowing()) {
            notificationPopupStage.close();
        }
    }
}




