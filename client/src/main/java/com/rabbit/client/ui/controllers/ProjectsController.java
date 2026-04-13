package com.rabbit.client.ui.controllers;

import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class ProjectsController {

    @FXML private FlowPane recentProjectsPane;
    @FXML private FlowPane yourProjectsPane;

    @FXML
    public void initialize() {
        List<String> recentProjects = List.of("PROJECT_NAME_1", "PROJECT_NAME_2");
        List<String> yourProjects = List.of("PROJECT_NAME_1", "PROJECT_NAME_2", "PROJECT_NAME_3");

        for (String name : recentProjects) {
            recentProjectsPane.getChildren().add(createProjectCard(name));
        }
        for (String name : yourProjects) {
            yourProjectsPane.getChildren().add(createProjectCard(name));
        }
    }

    private VBox createProjectCard(String name) {
        VBox card = new VBox(10);
        card.setPrefWidth(220);
        card.setPrefHeight(120);
        card.setStyle("-fx-background-color: #0d2137; -fx-background-radius: 8; -fx-padding: 15;");

        Label title = new Label(name);
        title.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button openTasks = new Button("VIEW OPEN TASKS");
        openTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        Button assignedTasks = new Button("VIEW ASSIGNED TASKS");
        assignedTasks.setStyle("-fx-background-color: transparent; -fx-text-fill: #fcfcfc; -fx-padding: 0; -fx-cursor: hand;");

        card.getChildren().addAll(title, openTasks, assignedTasks);
        return card;
    }
}