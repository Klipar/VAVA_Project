package com.rabbit.client.ui.controllers;

import java.io.IOException;
import com.rabbit.client.service.UserService;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;

public class MainController {
    @Getter
    private static MainController instance;
    @FXML private BorderPane rootPane;
    @FXML private SidebarController sidebarController;
    @FXML private StackPane overlayPane;
    private final UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        instance = this;
        com.rabbit.client.Config.getInstance().setMainController(this);
        if (!userService.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        if (sidebarController != null) {
            sidebarController.setMainController(this);
            sidebarController.initMenu();
        }
        loadView("home-view.fxml");
    }

    private void redirectToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/login_page.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());
            Stage stage = (Stage) rootPane.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void loadView(String fxmlName) {
        loadView(fxmlName, null, null);
    }

    public void loadView(String fxmlName, Integer projectId, String projectName) {
        try {
            String path = "/com/rabbit/client/fxml/" + fxmlName;
            FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
            Parent view = loader.load();

            Object controller = loader.getController();

            if (controller instanceof BoardController board) {
                board.setMainController(this);
                if (projectId != null) {
                    board.setCurrentProject(projectId, projectName);
                }
            } else if (controller instanceof HomePageController home) {
                home.setMainController(this);
            } else if (controller instanceof ProjectsTaskController projectTasks) {
                projectTasks.setMainController(this);
                if (projectId != null && projectName != null) {
                    projectTasks.setProject(projectId, projectName);
                }
            } else if (controller instanceof MyTasksController myTasks) {
                myTasks.setMainController(this);
            }

            rootPane.setCenter(view);
        } catch (IOException e) {
            System.err.println("Помилка завантаження FXML: " + fxmlName);
            e.printStackTrace();
        }
    }

    public void openProjectTasks(int projectId, String projectTitle) {
        loadView("project-tasks-view.fxml", projectId, projectTitle);
    }

    public void setView(Parent view) {
        if (!userService.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        rootPane.setCenter(view);
    }

    public void showGlobalNotification(String message, String colorHex) {
        Platform.runLater(() -> {
            Label notification = new Label(message);
            notification.setStyle("-fx-background-color: " + colorHex + "; " +
                    "-fx-text-fill: white; -fx-padding: 15 30; " +
                    "-fx-background-radius: 10; -fx-font-weight: bold; " +
                    "-fx-font-size: 14px; " +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 4);");

            notification.setMouseTransparent(true);

            overlayPane.getChildren().add(notification);
            notification.toFront();

            StackPane.setAlignment(notification, Pos.TOP_CENTER);
            StackPane.setMargin(notification, new javafx.geometry.Insets(50, 0, 0, 0));

            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), notification);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), notification);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setDelay(Duration.seconds(2.5));
            fadeOut.setOnFinished(e -> overlayPane.getChildren().remove(notification));

            fadeIn.play();
            fadeOut.play();
        });
    }
}