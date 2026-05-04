package com.rabbit.client;

import com.rabbit.client.service.UserService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        UserService userService = UserService.getInstance();
        userService.loadSavedSession();

        String fxmlPath;
        if (userService.isLoggedIn()) {
            if (userService.getCurrentUser().getSkills() == null ||
                    userService.getCurrentUser().getSkills().isBlank()) {
                fxmlPath = "/com/rabbit/client/fxml/first_input_page.fxml";
            } else {
                fxmlPath = "/com/rabbit/client/fxml/main-view.fxml";
            }
        } else {
            fxmlPath = "/com/rabbit/client/fxml/login_page.fxml";
        }

        FXMLLoader fxmlLoader = new FXMLLoader(
            getClass().getResource(fxmlPath),
            com.rabbit.client.Config.getInstance().getBundle()
        );
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());

        stage.setTitle("Rabbit Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}