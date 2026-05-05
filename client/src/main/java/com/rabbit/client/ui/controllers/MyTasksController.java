package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.TaskDto;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.Setter;

import java.util.*;

public class MyTasksController {

    @FXML private StackPane rootStackPane;
    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> titleColumn, descriptionColumn,
            statusColumn, projectColumn, deadlineColumn;
    @FXML private Label statusLabel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final ApiClient apiClient = ApiClient.getInstance();
    private final Map<Integer, String> projectNames = new HashMap<>();

    @Setter
    private MainController mainController;

    @FXML
    public void initialize() {
        setupColumns();
        setupRowClickHandler();
        loadTasksAsync();
    }

    public void loadTasksAsync() {
        statusLabel.setText("Loading...");
        new Thread(() -> {
            try {
                var projectResp = apiClient.get("/projects");
                if (!apiClient.isSuccess(projectResp)) {
                    Platform.runLater(() ->
                            statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_projects")));
                    return;
                }

                List<Map<String, Object>> projects =
                        mapper.readValue(projectResp.body(), new TypeReference<>() {});

                projectNames.clear();
                for (Map<String, Object> p : projects) {
                    projectNames.put((int) p.get("id"), (String) p.get("title"));
                }

                Long currentUserId = Config.getInstance().getUser().getId();
                List<TaskDto> myTasks = new ArrayList<>();

                for (Map<String, Object> project : projects) {
                    int projectId = (int) project.get("id");
                    var taskResp = apiClient.get("/tasks/" + projectId);
                    if (apiClient.isSuccess(taskResp)) {
                        List<TaskDto> tasks =
                                mapper.readValue(taskResp.body(), new TypeReference<>() {});
                        for (TaskDto task : tasks) {
                            if (task.getAssignedTo() == currentUserId.intValue()) {
                                task.setProjectId(projectId);
                                myTasks.add(task);
                            }
                        }
                    }
                }

                ObservableList<TaskDto> observable = FXCollections.observableArrayList(myTasks);
                Platform.runLater(() -> {
                    tasksTable.setItems(observable);
                    statusLabel.setText(Config.getInstance().getBundle().getString("tasks_loaded"));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() ->
                        statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_tasks")));
            }
        }).start();
    }

    private void setupRowClickHandler() {
        tasksTable.setRowFactory(tv -> {
            TableRow<TaskDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 1) {
                    openTaskDetailPopup(row.getItem());
                }
            });
            return row;
        });
    }

    private void openTaskDetailPopup(TaskDto task) {
        if (rootStackPane == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/rabbit/client/fxml/task-detail-popup.fxml"),
                    Config.getInstance().getBundle()
            );
            Pane overlay = loader.load();
            TaskDetailPopupController controller = loader.getController();
            // My Tasks = read-only view (isMaster=false, no edit pencil)
            controller.setup(task, false, task.getProjectId(), this::loadTasksAsync);
            rootStackPane.getChildren().add(overlay);
            overlay.prefWidthProperty().bind(rootStackPane.widthProperty());
            overlay.prefHeightProperty().bind(rootStackPane.heightProperty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupColumns() {
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle()));

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription()));
        descriptionColumn.setPrefWidth(220);

        statusColumn.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getStatus();
            return new SimpleStringProperty(raw != null ? raw.toUpperCase().replace("_", " ") : "");
        });
        statusColumn.setPrefWidth(110);

        projectColumn.setCellValueFactory(cellData -> {
            int pid = cellData.getValue().getProjectId();
            return new SimpleStringProperty(projectNames.getOrDefault(pid, ""));
        });

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
                    dateLabel.setText(formatDeadline(deadlineStr));
                    container.getChildren().clear();
                    if (warningIcon != null) container.getChildren().add(warningIcon);
                    container.getChildren().add(dateLabel);
                    setGraphic(container);
                }
            }
        });
        deadlineColumn.setPrefWidth(180);
    }

    private String formatDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isBlank() || "null".equalsIgnoreCase(deadlineStr)) {
            return Config.getInstance().getBundle().getString("no_deadline");
        }
        try {
            if (!deadlineStr.endsWith("Z") && !deadlineStr.contains("+")) deadlineStr += "Z";
            java.time.ZonedDateTime zdt = java.time.ZonedDateTime.parse(deadlineStr);
            return zdt.format(Config.getInstance().getDateTimeFormatter()).toUpperCase();
        } catch (Exception e) {
            return deadlineStr.toUpperCase();
        }
    }
}