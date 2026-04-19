package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    // Прибираємо skillsLabel та nicknameLabel, бо їх немає у FXML

    private final UserService userService = UserService.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    public void initialize() {
        loadUserProfile();
    }

    private void loadUserProfile() {
        UserDto currentUser = userService.getCurrentUser();

        if (currentUser != null) {
            nameLabel.setText("Name: " + (currentUser.getName() != null ? currentUser.getName() : "Not set"));
            if (currentUser.getNickname() != null) {
                nameLabel.setText(nameLabel.getText() + " (@" + currentUser.getNickname() + ")");
            }
            emailLabel.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "No email"));
            roleLabel.setText("Role: " + (currentUser.getRole() != null ? currentUser.getRole().toString() : "No role"));

            // Додаємо skills як окремий Label або додаємо до nameLabel
            if (currentUser.getSkills() != null && !currentUser.getSkills().isEmpty()) {
                // Можна додати ще один Label для skills, або додати до існуючого
                // Поки просто виведемо в консоль
                System.out.println("Skills: " + currentUser.getSkills());
            }
        } else {
            nameLabel.setText("No user data found. Please login.");
            emailLabel.setText("");
            roleLabel.setText("");
        }
    }

    // Метод для оновлення профілю (можна викликати після редагування)
    public void refreshProfile() {
        loadUserProfile();
    }
}