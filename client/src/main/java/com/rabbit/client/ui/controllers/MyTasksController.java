package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.Config;
import com.rabbit.common.dto.TaskDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MyTasksController {

    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> titleColumn, descriptionColumn,
            statusColumn, projectColumn, deadlineColumn;
    @FXML private Label statusLabel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<Integer, String> projectNames = new HashMap<>();

    @Setter
    private MainController mainController;
    private DateTimeFormatter displayFormatter;

    @FXML
    public void initialize() {
        displayFormatter = Config.getInstance().getDateTimeFormatter();
        setupColumns();
        loadTask();
    }

    private void loadTask() {
        String token = Config.getInstance().getToken();
        if(token == null || token.isEmpty()){
            statusLabel.setText(Config.getInstance().getBundle().getString("please_login"));
            return;
        }

        try {
            HttpRequest projectRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/projects"))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> projectRes = client.send(projectRequest,HttpResponse.BodyHandlers.ofString());

            if(projectRes.statusCode() != 200){
                statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_projects"));
                return;
            }

            List<Map<String, Object>> projects = mapper.readValue(projectRes.body(), new TypeReference<>() {});

            projectNames.clear();
            for(Map<String, Object> project : projects){
                projectNames.put((int) project.get("id"), (String) project.get("title"));
            }

            Long currentUserId = Config.getInstance().getUser().getId();
            List<TaskDto> myTasks = new ArrayList<>();

            for(Map<String, Object> project : projects){
                int projectId = (int) project.get("id");
                String projectName = (String) project.get("title");
                HttpRequest taskRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:6969/tasks/" + projectId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();
                HttpResponse<String> tasksResponce = client.send(taskRequest,HttpResponse.BodyHandlers.ofString());
                if(tasksResponce.statusCode() == 200){
                    List<TaskDto> tasks = mapper.readValue(tasksResponce.body(), new TypeReference<>() {});
                    for (TaskDto task : tasks) {
                        if (task.getAssignedTo() == currentUserId.intValue()){
                            task.setProjectId(projectId);
                            myTasks.add(task);
                        }
                    }
                }else {
                    System.out.println("Failed to load tasks for project: " + projectName);
                }
            }
            ObservableList<TaskDto> observableTasks = FXCollections.observableArrayList(myTasks);
            tasksTable.setItems(observableTasks);
            statusLabel.setText(Config.getInstance().getBundle().getString("tasks_loaded"));
        }catch (Exception e){
            e.printStackTrace();
            statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_tasks"));
        }
    }


    private void setupColumns() {
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle())
        );

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));
        descriptionColumn.setPrefWidth(220);

        statusColumn.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getStatus();
            return new SimpleStringProperty(raw.toUpperCase().replace("_", " "));
        });
        statusColumn.setPrefWidth(110);

        deadlineColumn.setCellFactory(column -> new TableCell<>() {
            private final HBox container = new HBox(8);
            private final Label dateLabel = new Label();
            private ImageView warningIcon;

            {
                container.setAlignment(Pos.CENTER_LEFT);
                var stream = getClass().getResourceAsStream("/com/rabbit/client/images/worning.png");
                if (stream != null) {
                    warningIcon = new ImageView(new Image(stream));
                    warningIcon.setFitWidth(16);
                    warningIcon.setFitHeight(16);
                }
                dateLabel.setStyle("-fx-text-fill: #99AAB5;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String deadlineStr = getTableRow().getItem().getDeadline();
                    String formattedDate = Config.getInstance().getBundle().getString("no_deadline");

                    if (deadlineStr != null && !deadlineStr.isBlank() && !"null".equalsIgnoreCase(deadlineStr)) {
                        try {
                            if (!deadlineStr.endsWith("Z") && !deadlineStr.contains("+")) deadlineStr += "Z";
                            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(deadlineStr);
                            formattedDate = zdt.format(displayFormatter).toUpperCase();
                        } catch (Exception e) {
                            formattedDate = deadlineStr.toUpperCase();
                        }
                    }

                    dateLabel.setText(formattedDate);
                    container.getChildren().clear();
                    if (warningIcon != null) container.getChildren().add(warningIcon);
                    container.getChildren().add(dateLabel);
                    setGraphic(container);
                }
            }
        });
        deadlineColumn.setPrefWidth(180);
    }
}