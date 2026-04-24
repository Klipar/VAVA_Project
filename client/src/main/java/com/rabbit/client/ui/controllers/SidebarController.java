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

        createButton(rb.getString("home_page"), "home-view.fxml", "home.png");
        createButton(rb.getString("my_projects_"), "projects-view.fxml", "folder.png");

        if (user.getRole() != UserRole.MANAGER)
            createButton(rb.getString("my_tasks"), "my-tasks-view.fxml", "task.png");

        if (user.getRole() == UserRole.MANAGER || user.getRole() == UserRole.TEAM_LEADER)
            createButton(rb.getString("admin_panel"), "admin-view.fxml", "settings.png");

        createButton(rb.getString("notifications"), "notifications-view.fxml", "notificatios.png");
        createButton(rb.getString("profile"), "profile-view.fxml", "person.png");

        change_language.setOnAction(event -> {
            if (mainController != null) {
                System.out.println("Change language!!!");
                // mainController.loadView();
            }
        });
    }

    private void createButton(String text, String fxmlName, String iconName) {
        Button btn = new Button(text);

        try {
            String imagePath = "/com/rabbit/client/images/icons/" + iconName;
            javafx.scene.image.Image image = new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath));
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);


            imageView.setFitWidth(20);
            imageView.setFitHeight(20);

            btn.setGraphic(imageView);

            btn.setGraphicTextGap(10);
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconName);
        }

        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);

        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        btn.setOnAction(event -> {
            if (mainController != null) {
                mainController.loadView(fxmlName);
            }
        });

        menuItemsContainer.getChildren().add(btn);
    }
}