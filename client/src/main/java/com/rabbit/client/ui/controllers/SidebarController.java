package com.rabbit.client.ui.controllers;

import java.util.Objects;
import java.util.ResourceBundle;

import com.rabbit.client.Config;
import com.rabbit.client.service.NotificationPollingService;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import lombok.Setter;

public class SidebarController {
    @FXML private VBox menuItemsContainer;
    @FXML private Button languageBtn;

    @Setter
    private MainController mainController;
    private Popup languagePopup;

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

        createNotificationButton(rb.getString("notifications"), "notificatios.png");
        createButton(rb.getString("profile"), "profile-view.fxml", "person.png");

        setupLanguageButton(rb.getString("language_btn"));
    }

    private void setupLanguageButton(String text) {
        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/icons/language.png")));
            ImageView iv = new ImageView(img);
            iv.setFitWidth(18);
            iv.setFitHeight(18);
            iv.getStyleClass().add("button-icon");
            languageBtn.setGraphic(iv);
        } catch (Exception e) {
            System.err.println("Language icon not found");
        }
        languageBtn.setGraphicTextGap(12);
        languageBtn.setText(text);
        languageBtn.setAlignment(Pos.CENTER_LEFT);
        languageBtn.setOnAction(event -> toggleLanguagePopup());
    }

    private void createButton(String text, String fxmlName, String iconName) {
        Button btn = new Button(text);

        try {
            String imagePath = "/com/rabbit/client/images/icons/" + iconName;
            Image image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(18);
            imageView.setFitHeight(18);
            imageView.getStyleClass().add("button-icon");
            btn.setGraphic(imageView);
            btn.setGraphicTextGap(12);
        } catch (Exception e) {
            System.err.println("Icon not found: " + iconName);
        }

        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);

        btn.setOnAction(event -> {
            if (mainController != null) mainController.loadView(fxmlName);
        });

        menuItemsContainer.getChildren().add(btn);
    }

    private void createNotificationButton(String text, String iconName) {
        Button btn = new Button(text);

        try {
            String imagePath = "/com/rabbit/client/images/icons/" + iconName;
            javafx.scene.image.Image image = new javafx.scene.image.Image(getClass().getResourceAsStream(imagePath));
            javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);

            imageView.setFitWidth(18);
            imageView.setFitHeight(18);

            imageView.getStyleClass().add("button-icon");

            btn.setGraphic(imageView);
            btn.setGraphicTextGap(12);
        } catch (Exception e) {
            System.err.println("Icon not found: " + iconName);
        }

        btn.getStyleClass().add("sidebar-button");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        btn.setOnAction(event -> {
            NotificationPollingService.getInstance().showNotificationPopup();
        });

        menuItemsContainer.getChildren().add(btn);
    }

    private void toggleLanguagePopup() {
        if (languagePopup != null && languagePopup.isShowing()) {
            languagePopup.hide();
            return;
        }
        showLanguagePopup();
    }

    private void showLanguagePopup() {
        String current = Config.getInstance().getCurrentLocale().getLanguage();

        VBox container = new VBox();
        container.setStyle(
                "-fx-background-color: #0d1e30;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: #1e3a5f;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 12, 0, 0, 4);"
        );

        double btnW = languageBtn.getWidth();
        container.setPrefWidth(btnW);
        container.setMinWidth(btnW);
        container.setMaxWidth(btnW);

        container.getChildren().add(buildLanguageRow("ENGLISH", "en", current.equals("en")));

        javafx.scene.layout.Region sep = new javafx.scene.layout.Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: rgba(107,114,128,0.4);");
        container.getChildren().add(sep);

        container.getChildren().add(buildLanguageRow("SLOVAK", "sk", current.equals("sk")));

        languagePopup = new Popup();
        languagePopup.setAutoHide(true);
        languagePopup.getContent().add(container);

        double btnX = languageBtn.localToScreen(0, 0).getX();
        double btnY = languageBtn.localToScreen(0, 0).getY();

        languagePopup.show(languageBtn.getScene().getWindow(), btnX, btnY);

        double popupHeight = languagePopup.getHeight();
        languagePopup.setX(btnX - 16);
        languagePopup.setY(btnY - popupHeight + 12);
    }

    private HBox buildLanguageRow(String name, String localeCode, boolean selected) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setStyle("-fx-cursor: hand;");

        Circle outer = new Circle(8);
        outer.setFill(Color.TRANSPARENT);
        outer.setStroke(selected ? Color.web("#4a9eda") : Color.web("#5a7a9a"));
        outer.setStrokeWidth(2);

        if (selected) {
            Circle inner = new Circle(4);
            inner.setFill(Color.web("#4a9eda"));
            javafx.scene.layout.StackPane radio = new javafx.scene.layout.StackPane(outer, inner);
            row.getChildren().add(radio);
        } else {
            row.getChildren().add(outer);
        }

        Label label = new Label(name);
        label.setStyle(
            "-fx-text-fill: " + (selected ? "#ffffff" : "#8a9ab5") + ";" +
            "-fx-font-size: 13px;" +
            "-fx-font-weight: " + (selected ? "bold" : "normal") + ";"
        );
        row.getChildren().add(label);

        if (!selected) {
            row.setOnMouseEntered(e -> row.setStyle("-fx-background-color: #112233; -fx-cursor: hand;"));
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));
            row.setOnMouseClicked(e -> {
                Config.getInstance().setLocale(localeCode);
                languagePopup.hide();
                initMenu();
                if (mainController != null) mainController.reloadCurrentView();
            });
        }

        return row;
    }
}
