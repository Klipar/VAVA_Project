package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class SidebarController {
    @FXML private VBox menuItemsContainer;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void initMenu() {
        menuItemsContainer.getChildren().clear();
        UserDto user = Config.getInstance().getUser();


        createButton("HOME PAGE", "home-view.fxml");
        createButton("MY PROJECTS", "projects-view.fxml");

        if (user.getRole() != UserRole.MANAGER)
            createButton("MY TASKS", "my-tasks-view.fxml");

        if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.TEAM_LEADER)
            createButton("ADMIN PANEL", "admin-view.fxml");

        createButton("NOTIFICATIONS", "notifications-view.fxml");
        createButton("PROFILE", "profile-view.fxml");
    }

    private void createButton(String text, String fxmlName) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setOnAction(event -> {
            if (mainController != null) {
                mainController.loadView(fxmlName);
            }
        });

        menuItemsContainer.getChildren().add(btn);
    }
}