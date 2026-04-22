package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.enums.UserRole;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ProjectsTaskController {

    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> typeColumn, titleColumn, descriptionColumn,statusColumn, assigneeColumn, deadlineColumn;
    @FXML private Label statusLabel;
    @FXML private Button boardBtn, listBtn, tasksBtn, topCreateTaskBtn;
    @FXML private Label projectTitleLabel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    private int currentProjectId;
    private String currentProjectTitle;
    @Setter
    private MainController mainController;

    public void setProject(int projectId, String projectTitle) {
        this.currentProjectId = projectId;
        this.currentProjectTitle = projectTitle;
        projectTitleLabel.setText(projectTitle.toUpperCase());
        loadTasksForProject();

        if (mainController != null) {
            boardBtn.setOnAction(e ->
                    mainController.loadView("board-page.fxml", currentProjectId, currentProjectTitle));
            listBtn.setOnAction(e ->
                    mainController.loadView("project-tasks-view.fxml", currentProjectId, currentProjectTitle));
            tasksBtn.setOnAction(e -> mainController.loadView("my-tasks-view.fxml"));
        }
        boardBtn.getStyleClass().remove("active-nav-button");
        listBtn.getStyleClass().add("active-nav-button");
        tasksBtn.getStyleClass().remove("active-nav-button");

        boolean isAdmin = UserService.getInstance().getCurrentUser().getRole() == UserRole.MANAGER ||
                UserService.getInstance().getCurrentUser().getRole() == UserRole.TEAM_LEADER;
        topCreateTaskBtn.setVisible(isAdmin);
        topCreateTaskBtn.setManaged(isAdmin);
    }


    @FXML
    public void initialize() {
        setupColumns();
    }

    private void loadTasksForProject() {
        String token = Config.getInstance().getToken();
        if(token == null || token.isEmpty()){
            statusLabel.setText("Please login to view tasks");
            return;
        }

        try {
            HttpRequest taskRequest = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:6969/tasks/" + currentProjectId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(taskRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200){
                statusLabel.setText("Failed to load tasks");
                return;
            }
            List<TaskDto> tasks = mapper.readValue(response.body(), new TypeReference<>() {});
            ObservableList<TaskDto> observableTasks = FXCollections.observableArrayList(tasks);
            tasksTable.setItems(observableTasks);
            statusLabel.setText("Tasks in project " + currentProjectTitle);
        }catch (Exception e){
            e.printStackTrace();
            statusLabel.setText("Failed to load tasks");
        }
    }

    private void setupColumns() {
        typeColumn.setCellValueFactory(cellData -> {
            int priority = cellData.getValue().getPriority();
            String type = priority >= 4 ? "BUGS" : "FEATURE";
            return new SimpleStringProperty(type);
        });

        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle() != null ? cellData.getValue().getTitle().toUpperCase() : ""));

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription() != null ? cellData.getValue().getDescription() : ""));

        statusColumn.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getStatus();
            return new SimpleStringProperty(raw != null ? raw.toUpperCase().replace("_", " ") : "");
        });

        assigneeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAssignedTo() > 0 ? "Assigned" : "Unassigned"));

        deadlineColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDeadline() != null ? cellData.getValue().getDeadline() : "No deadline"));

        tasksTable.setRowFactory(tv -> {
            TableRow<TaskDto> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty() && mainController != null) {
                    TaskDto task = row.getItem();
                    mainController.loadView("board-page.fxml", task.getProjectId(), currentProjectTitle);
                }
            });
            return row;
        });
    }
}