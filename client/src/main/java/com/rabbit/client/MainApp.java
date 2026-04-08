package com.rabbit.client;

import com.rabbit.common.dto.UserDto;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        Config conf = Config.getInstance();
        UserDto user = UserDto.fromJson("{\"id\": 1,\"name\": \"Bober\",\"nickname\": \"boberto\",\"email\": \"boberto@example.com\",\"role\": \"TEAM_LEADER\",\"createdAt\": \"1831-04-08T18:00:00\"}");
        conf.setUser(user);

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);

        scene.getStylesheets().add(getClass().getResource("/com/rabbit/client/css/style.css").toExternalForm());

        stage.setTitle("Rabbit Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
