package com.rabbit.client.ui.controllers;

import java.io.IOException;
import com.rabbit.client.service.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainController {
    @FXML private BorderPane rootPane;
    @FXML private SidebarController sidebarController;
    private final UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
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
        if (!userService.isLoggedIn()) {
            redirectToLogin();
            return;
        }
        try {
            String path = "/com/rabbit/client/fxml/" + fxmlName;
            Parent view = FXMLLoader.load(getClass().getResource(path));
            rootPane.setCenter(view);
        } catch (IOException e) {
            System.err.println("Failed to load FXML: " + fxmlName);
            e.printStackTrace();
        }
    }
}