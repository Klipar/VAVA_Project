package com.rabbit.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/first_input_page.fxml"));
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
