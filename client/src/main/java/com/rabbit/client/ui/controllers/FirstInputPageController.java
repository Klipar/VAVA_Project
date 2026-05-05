package com.rabbit.client.ui.controllers;

import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
import javafx.application.Platform;
import javafx.fxml.FXML;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
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

            HttpResponse<String> updateResponse = apiClient.put(
                    String.format("/users/%d/update", user_id),
                    updateBody
            );
            System.out.println("Update status: " + updateResponse.statusCode());

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

    @FXML
    void handleImportSkills() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Skills To Import");
        chooser.setInitialDirectory(new File(System.getProperty("user.home")));
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("XML files (*.xml)", "*.xml")
        );

        Stage stage = (Stage) continueFirstBtn.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);

        System.out.println("File selected: " + file);
        if (file == null) return;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            doc.getDocumentElement().normalize();

            String name = "";
            String nickname = "";
            String skills = "";

            NodeList nameNodes = doc.getElementsByTagName("name");
            if (nameNodes.getLength() > 0)
                name = nameNodes.item(0).getTextContent().trim();

            NodeList nicknameNodes = doc.getElementsByTagName("nickname");
            if (nicknameNodes.getLength() > 0)
                nickname = nicknameNodes.item(0).getTextContent().trim();

            NodeList skillsNodes = doc.getElementsByTagName("skills");
            if (skillsNodes.getLength() > 0)
                skills = skillsNodes.item(0).getTextContent().trim();


            final String finalName = name;
            final String finalNickname = nickname;
            final String finalSkills = skills;

            Platform.runLater(() -> {
                fullNameField.setText(finalName);
                nicknameField.setText(finalNickname);
                skillsArea.setText(finalSkills);
            });

        } catch (Exception e) {
            System.err.println("Failed to import: " + e.getMessage());
            e.printStackTrace();
        }
    }
}