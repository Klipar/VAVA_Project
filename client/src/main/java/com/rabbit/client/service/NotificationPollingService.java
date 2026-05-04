package com.rabbit.client.service;

import com.rabbit.client.ui.controllers.MainController;
import com.rabbit.common.dto.NotificationDto;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NotificationPollingService {
    private static NotificationPollingService instance;
    private final ApiClient apiClient;
    private final ScheduledExecutorService executor;
    private final Set<Long> shownNotificationIds;
    private boolean isRunning;
    private boolean isFirstPoll;
    private List<NotificationDto> currentNotifications;
    private Pane overlayPane;

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
        this.isFirstPoll = true;
        this.currentNotifications = new ArrayList<>();
    }

    public static synchronized NotificationPollingService getInstance() {
        if (instance == null) {
            instance = new NotificationPollingService();
        }
        return instance;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        pollNotifications();
        executor.scheduleAtFixedRate(this::pollNotifications,
                POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!isRunning) return;
        isRunning = false;
        executor.shutdown();
        try { if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }

    private void pollNotifications() {
        try {
            List<NotificationDto> notifications = apiClient.getNotifications();
            Platform.runLater(() -> {
                if (notifications != null) {
                    if (isFirstPoll) {
                        for (NotificationDto n : notifications) {
                            if (n.getId() != null) shownNotificationIds.add(n.getId());
                        }
                        isFirstPoll = false;
                    } else {
                        for (NotificationDto n : notifications) {
                            if (n.getId() != null && !shownNotificationIds.contains(n.getId())
                                    && Boolean.FALSE.equals(n.getIsRead())) {
                                MainController controller = MainController.getInstance();
                                if (controller != null) {
                                    controller.showGlobalNotification(n.getMessage(),
                                            determineNotificationColor(n.getMessage()));
                                }
                                shownNotificationIds.add(n.getId());
                            }
                        }
                    }
                    currentNotifications = new ArrayList<>(notifications);
                }
            });
        } catch (Exception ignored) {}
    }

    private String determineNotificationColor(String message) {
        if (message.toLowerCase().contains("error") || message.toLowerCase().contains("failed"))
            return "#ff6347";
        if (message.toLowerCase().contains("success") || message.toLowerCase().contains("completed"))
            return SUCCESS_COLOR;
        if (message.toLowerCase().contains("warning") || message.toLowerCase().contains("deadline"))
            return WARNING_COLOR;
        return INFO_COLOR;
    }

    public List<NotificationDto> getCurrentNotifications() {
        return new ArrayList<>(currentNotifications);
    }

    public void markAsRead(long notificationId, Runnable onComplete) {
        executor.execute(() -> {
            try {
                boolean success = apiClient.markNotificationAsRead(notificationId);
                if (success) {
                    currentNotifications = currentNotifications.stream()
                            .peek(n -> {
                                if (n.getId() != null && n.getId().equals(notificationId))
                                    n.setIsRead(true);
                            })
                            .toList();
                }
            } catch (Exception ignored) {}
            finally {
                if (onComplete != null) Platform.runLater(onComplete);
            }
        });
    }

    public void showNotificationPopup() {
        Platform.runLater(() -> {
            try {
                if (overlayPane != null && overlayPane.getParent() != null) return;

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/rabbit/client/fxml/notification-popup.fxml")
                );
                overlayPane = loader.load();

                MainController main = MainController.getInstance();
                if (main != null && main.getOverlayPane() != null) {
                    StackPane overlay = main.getOverlayPane();
                    overlay.getChildren().add(overlayPane);
                    overlayPane.toFront();
                }
            } catch (Exception ignored) {}
        });
    }

    public void onPopupClosed() {
        this.overlayPane = null;
    }
}