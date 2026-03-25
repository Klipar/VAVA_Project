package com.rabbit.client.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        Button button = new Button("Call server");

        button.setOnAction(e -> {
            // Async call to REST server
            new Thread(() -> {
                try {
                    String response = getHelloFromServer();
                    System.out.println(response);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        Scene scene = new Scene(button, 300, 200);
        stage.setScene(scene);
        stage.setTitle("Rabbit Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    private String getHelloFromServer() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:6969/hello"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}