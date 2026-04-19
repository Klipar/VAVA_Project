package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private Button logoutButton;

    private final UserService userService = UserService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        loadUserProfile();

        if (logoutButton != null) {
            logoutButton.setOnAction(event -> handleLogout());
        }
    }

    private void loadUserProfile() {
        UserDto currentUser = userService.getCurrentUser();

        if (currentUser != null) {
            String name = currentUser.getName() != null ? currentUser.getName() : "Not set";
            String nickname = currentUser.getNickname() != null ? " (@" + currentUser.getNickname() + ")" : "";
            nameLabel.setText("Name: " + name + nickname);
            emailLabel.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "No email"));
            roleLabel.setText("Role: " + (currentUser.getRole() != null ? currentUser.getRole().toString() : "No role"));
        } else {
            nameLabel.setText("No user data found. Please login.");
            emailLabel.setText("");
            roleLabel.setText("");
        }
    }

    @FXML
    private void handleLogout() {
        try {
            userService.logout();

            Config.getInstance().setToken(null);
            Config.getInstance().setUser(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/login_page.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshProfile() {
        loadUserProfile();
    }
}