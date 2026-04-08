package com.rabbit.client.ui.controllers;

import java.util.ResourceBundle;

import com.rabbit.client.Config;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class SidebarController {
    @FXML private VBox menuItemsContainer;
    @FXML private Button change_language;

    private MainController mainController;

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void initMenu() {
        menuItemsContainer.getChildren().clear();
        UserDto user = Config.getInstance().getUser();

        ResourceBundle rb = Config.getInstance().getBundle();


        createButton(rb.getString("home_page"), "home-view.fxml");
        createButton(rb.getString("my_projects_"), "projects-view.fxml");

        if (user.getRole() != UserRole.MANAGER)
            createButton(rb.getString("my_tasks"), "my-tasks-view.fxml");

        if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.TEAM_LEADER)
            createButton(rb.getString("admin_panel"), "admin-view.fxml");

        createButton(rb.getString("notifications"), "notifications-view.fxml");
        createButton(rb.getString("profile"), "profile-view.fxml");

        change_language.setOnAction(event -> {
            if (mainController != null) {
                System.out.println("Change language!!!");
                // mainController.loadView();
            }
        });
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