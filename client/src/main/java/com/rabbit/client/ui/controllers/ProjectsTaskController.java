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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ProjectsTaskController {

    @FXML private StackPane rootStackPane;
    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> titleColumn, descriptionColumn, statusColumn, assigneeColumn, deadlineColumn;
    @FXML private Label statusLabel;
    @FXML private Button boardBtn, listBtn, tasksBtn, topCreateTaskBtn;
    @FXML private Label projectTitleLabel;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();
    private final ApiClient apiClient = ApiClient.getInstance();

    private int currentProjectId;
    private String currentProjectTitle;
    private boolean isMaster = false;
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

        // Check project-level role asynchronously
        new Thread(() -> {
            try {
                var resp = apiClient.get("/projects/" + projectId + "/role");
                if (apiClient.isSuccess(resp)) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> result = mapper.readValue(resp.body(), Map.class);
                    String role = result.getOrDefault("role", "none");
                    Platform.runLater(() -> this.isMaster = "master".equals(role));
                }
            } catch (Exception ignored) {}
        }).start();

        setupRowClickHandler();
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
            controller.setup(task, isMaster, currentProjectId, this::loadTasksForProject);
            rootStackPane.getChildren().add(overlay);
            overlay.prefWidthProperty().bind(rootStackPane.widthProperty());
            overlay.prefHeightProperty().bind(rootStackPane.heightProperty());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    public void initialize() {
        setupColumns();
    }

    private void loadTasksForProject() {
        String token = Config.getInstance().getToken();
        if(token == null || token.isEmpty()){
            statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_tasks_login"));
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
                statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_tasks_msg"));
                return;
            }
            List<TaskDto> tasks = mapper.readValue(response.body(), new TypeReference<>() {});
            ObservableList<TaskDto> observableTasks = FXCollections.observableArrayList(tasks);
            tasksTable.setItems(observableTasks);
            statusLabel.setText(Config.getInstance().getBundle().getString("tasks_in_project") + " " + currentProjectTitle);
        }catch (Exception e){
            e.printStackTrace();
            statusLabel.setText(Config.getInstance().getBundle().getString("failed_load_tasks_msg"));
        }
    }

    private void setupColumns() {
        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle() != null ? cellData.getValue().getTitle().toUpperCase() : ""));

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription() != null ? cellData.getValue().getDescription() : ""));

        statusColumn.setCellValueFactory(cellData -> {
            String raw = cellData.getValue().getStatus();
            return new SimpleStringProperty(raw != null ? raw.toUpperCase().replace("_", " ") : "");
        });

        assigneeColumn.setCellValueFactory(cellData -> {
            java.util.ResourceBundle rb = Config.getInstance().getBundle();
            return new SimpleStringProperty(
                    cellData.getValue().getAssignedTo() > 0 ? rb.getString("assigned") : rb.getString("unassigned")
            );
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
                    String formattedDate = formatDeadline(deadlineStr);

                    dateLabel.setText(formattedDate);
                    container.getChildren().clear();
                    if (warningIcon != null) container.getChildren().add(warningIcon);
                    container.getChildren().add(dateLabel);
                    setGraphic(container);
                }
            }
        });
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