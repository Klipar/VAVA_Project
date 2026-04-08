package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;

    @FXML
    public void initialize() {
        UserDto currentUser = Config.getInstance().getUser();

        if (currentUser != null) {
            nameLabel.setText("Name: " + currentUser.getName() + " (@" + currentUser.getNickname() + ")");
            emailLabel.setText("Email: " + currentUser.getEmail());
            roleLabel.setText("Role: " + currentUser.getRole().toString());
        } else {
            nameLabel.setText("No user data found. Please login.");
        }
    }
}