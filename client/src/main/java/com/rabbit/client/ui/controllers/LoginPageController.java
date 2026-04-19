package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.http.HttpResponse;

public class LoginPageController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final UserService userService = UserService.getInstance();
    private final ApiClient apiClient = ApiClient.getInstance();

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }

        loginBtn.setDisable(true);
        errorLabel.setText("");

        try {
            String loginBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
            HttpResponse<String> loginResponse = apiClient.postPublic("/users/login", loginBody);

            if (loginResponse.statusCode() != 201) {
                errorLabel.setText("Invalid email or password");
                loginBtn.setDisable(false);
                return;
            }

            JsonNode root = mapper.readTree(loginResponse.body());
            String token = root.get("token").asText();
            UserDto loggedInUser = mapper.treeToValue(root.get("user"), UserDto.class);

            userService.login(token, loggedInUser);
            Config.getInstance().setToken(token);
            Config.getInstance().setUser(loggedInUser);

            String skills = loggedInUser.getSkills();
            String nextFxml = (skills == null || skills.isBlank())
                    ? "first_input_page.fxml"
                    : "main-view.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/" + nextFxml));
            Scene scene = new Scene(loader.load(), 1000, 700);
            scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());

            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Connection error. Is the server running?");
            loginBtn.setDisable(false);
        }
    }
}