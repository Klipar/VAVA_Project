package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FirstInputPageController
{
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextArea skillsArea;
    @FXML private Button continueFirstBtn;

    @FXML
    private void handleContinue() {
        String fullName = fullNameField.getText();
        String email = emailField.getText();
        String skills = skillsArea.getText();
        Long user_id = Config.getInstance().getUser().getId();

        if (fullName.isEmpty() || email.isEmpty() || skills.isEmpty()) {
            System.out.println("Please fill in all fields");
            return;
        }

        try {

            HttpClient client = HttpClient.newHttpClient();

            String token = Config.getInstance().getToken();

            String updateBody = String.format(
                    "{\"name\": \"%s\", \"nickname\": \"%s\", \"email\": \"%s\", \"skills\": \"%s\"}",
                    fullName, fullName.toLowerCase().replace(" ", ""), email, skills
            );

            HttpRequest updateRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("http://localhost:6969/users/%d/update", user_id)))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .PUT(HttpRequest.BodyPublishers.ofString(updateBody))
                    .build();

            HttpResponse<String> updateResponse = client.send(updateRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Update status: " + updateResponse.statusCode());

            HttpRequest getUserRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("http://localhost:6969/users/%d", user_id)))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> getUserResponse = client.send(getUserRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("User response: " + getUserResponse.body());

            UserDto user = new UserDto();
            user.setId(user_id);
            user.setName(fullName);
            user.setEmail(email);
            user.setNickname(fullName.toLowerCase().replace(" ", ""));
            user.setRole(Config.getInstance().getUser().getRole());
            user.setSkills(skills);

            Config.getInstance().setUser(user);
            System.out.println("Config user set: " + user.getName());

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/main-view.fxml"));
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());
            Stage stage = (Stage) continueFirstBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

