package com.rabbit.client.ui.controllers;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;

import com.rabbit.client.Config;

public class MainController {
    @FXML private BorderPane rootPane;

    // JavaFX automatically injects the controller of the nested FXML
    // The name must be: fx:id_in_fxml + "Controller"
    @FXML private SidebarController sidebarController;

    @FXML
    public void initialize() {
        if (sidebarController != null) {
            sidebarController.setMainController(this);
            sidebarController.initMenu();
        }

        Config.getInstance().setMainController(this);

        loadView("home-view.fxml");
    }

    public void loadView(String fxmlName) {
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