package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.common.dto.UserDto;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class LoginPageController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginBtn;
    @FXML private Label errorLabel;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please fill in all fields");
            return;
        }

        try {
            String loginBody = String.format("{\"email\": \"%s\", \"password\": \"%s\"}", email, password);
            HttpRequest loginRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/users/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                    .build();

            HttpResponse<String> loginResponse = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());

            if (loginResponse.statusCode() != 201) {
                errorLabel.setText("Invalid email or password");
                return;
            }


            JsonNode root = mapper.readTree(loginResponse.body());
            String token = root.get("token").asText();
            UserDto loggedInUser = mapper.treeToValue(root.get("user"), UserDto.class);

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
        }
    }
}