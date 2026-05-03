package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.http.HttpResponse;

public class FirstInputPageController
{
    @FXML private TextField fullNameField;
    @FXML private TextField nicknameField;
    @FXML private TextArea skillsArea;
    @FXML private Button continueFirstBtn;

    private final ApiClient apiClient = ApiClient.getInstance();

    @FXML
    private void handleContinue() {
        String fullName = fullNameField.getText();
        String nickname = nicknameField.getText();
        String skills = skillsArea.getText();
        Long user_id = Config.getInstance().getUser().getId();
        String user_email = Config.getInstance().getUser().getEmail();

        if (fullName.isEmpty() || nickname.isEmpty() || skills.isEmpty()) {
            System.out.println("Please fill in all fields");
            return;
        }

        try {

            String token = Config.getInstance().getToken();

            String updateBody = String.format(
                    "{\"name\": \"%s\", \"nickname\": \"%s\", \"email\": \"%s\", \"skills\": \"%s\"}",
                    fullName, nickname, user_email, skills
            );

            // Using ApiClient for updates
            HttpResponse<String> updateResponse = apiClient.put(
                    String.format("/users/%d/update", user_id),
                    updateBody
            );
            System.out.println("Update status: " + updateResponse.statusCode());

            // Using ApiClient for obtaining user
            HttpResponse<String> getUserResponse = apiClient.get(
                    String.format("/users/%d", user_id)
            );
            System.out.println("User response: " + getUserResponse.body());

            UserDto user = Config.getInstance().getUser();
            user.setName(fullName);
            user.setNickname(nickname);
            user.setSkills(skills);

            Config.getInstance().setUser(user);
            System.out.println("Config user set: " + user.getName());

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/rabbit/client/fxml/main-view.fxml"),
                Config.getInstance().getBundle()
            );
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