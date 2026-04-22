package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.Config;
import com.rabbit.common.dto.TaskDto;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
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
    @FXML private TableColumn<TaskDto, String> typeColumn, titleColumn, descriptionColumn,
            statusColumn, projectColumn, deadlineColumn;
    @FXML private Label statusLabel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final Map<Integer, String> projectNames = new HashMap<>();

    @Setter
    private MainController mainController;
    private final DateTimeFormatter displayFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);
    @FXML
    public void initialize() {
        setupColumns();
        loadTask();
        tasksTable.setRowFactory(tv -> {
            TableRow<TaskDto> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && mainController != null) {
                    TaskDto task = row.getItem();
                    if (task != null && task.getProjectId() > 0) {
                        String projectName = projectNames.getOrDefault(task.getProjectId(), "Project");
                        mainController.loadView("board-page.fxml", task.getProjectId(), projectName);
                    }
                }
            });
            return row;
        });
    }

    private void loadTask() {
        String token = Config.getInstance().getToken();
        if(token == null || token.isEmpty()){
            statusLabel.setText("Please login to view your tasks");
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
                statusLabel.setText("Failed to load projects");
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
                    System.out.println("Failed to load tasks f0r project: " + projectName);
                }
            }
            ObservableList<TaskDto> observableTasks = FXCollections.observableArrayList(myTasks);
            tasksTable.setItems(observableTasks);
            statusLabel.setText("Tasks loaded");
        }catch (Exception e){
            e.printStackTrace();
            statusLabel.setText("Failed to load tasks");
        }
    }


    private void setupColumns() {
        typeColumn.setCellValueFactory(cellData -> {
            int priority = cellData.getValue().getPriority();
            String type = priority > 2 ? "Bug" : "Feature";
            return new SimpleStringProperty(type);
        });

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

        projectColumn.setCellValueFactory(cellData -> {
            int projectId = cellData.getValue().getProjectId();
            String name = projectNames.getOrDefault(projectId, "Project #" + projectId);
            return new SimpleStringProperty(name);
        });
        projectColumn.setPrefWidth(160);

        deadlineColumn.setCellValueFactory(cellData -> {
            String deadlineStr = cellData.getValue().getDeadline();
            if (deadlineStr == null || deadlineStr.isBlank() || "null".equalsIgnoreCase(deadlineStr)) {
                return new SimpleStringProperty("No deadline");
            }
            try {
                if (!deadlineStr.endsWith("Z") && !deadlineStr.contains("+") && !deadlineStr.contains("GMT")) {
                    deadlineStr += "Z";
                }
                ZonedDateTime zdt = ZonedDateTime.parse(deadlineStr);
                return new SimpleStringProperty(zdt.format(displayFormatter));
            } catch (Exception e) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(deadlineStr);
                    return new SimpleStringProperty(ldt.atZone(ZoneId.of("UTC")).format(displayFormatter));
                } catch (Exception e2) {
                    return new SimpleStringProperty(deadlineStr);
                }
            }
        });
        deadlineColumn.setPrefWidth(140);
    }
}