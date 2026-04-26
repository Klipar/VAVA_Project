package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.component.TaskCardComponent;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.enums.TaskStatus;
import com.rabbit.common.enums.UserRole;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import lombok.Setter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BoardController {

    @FXML private StackPane rootStackPane;
    @FXML private HBox boardHBox;
    @FXML private Label projectNameLabel;
    @FXML private Button topCreateTaskBtn;
    @FXML private Button floatingAddBtn;

    @FXML private Button boardBtn;
    @FXML private Button listBtn;
    @FXML private Button tasksBtn;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Map<TaskStatus, VBox> columnContainers = new HashMap<>();
    @Setter
    private MainController mainController;

    private int currentProjectId;
    private String projectName;

    public void setCurrentProject(int id, String name) {
        this.currentProjectId = id;
        this.projectName = name;

        projectNameLabel.setText(projectName.toUpperCase());
        setupUserPermissions();
        createColumns();
        loadTasksFromServer();
        setupNavigationButtons();
    }

    @FXML
    public void initialize() {
    }

    private void setupNavigationButtons() {
        if (mainController == null) return;
        if (boardBtn != null) {
            boardBtn.getStyleClass().add("active-nav-button");
        }
        if (listBtn != null) {
            listBtn.setOnAction(e ->
                    mainController.loadView("project-tasks-view.fxml", currentProjectId, projectName)
            );
        }
        if (tasksBtn != null) {
            tasksBtn.setOnAction(e ->
                    mainController.loadView("my-tasks-view.fxml")
            );
        }
    }

    private void setupUserPermissions() {
        UserRole role = UserService.getInstance().getCurrentUser().getRole();
        boolean isManager = (role == UserRole.MANAGER || role == UserRole.TEAM_LEADER);

        topCreateTaskBtn.setVisible(isManager);
        topCreateTaskBtn.setManaged(isManager);
        floatingAddBtn.setVisible(isManager);
        floatingAddBtn.setManaged(isManager);

        if (isManager) {
            try {
                Image addIcon = new Image(getClass().getResourceAsStream("/com/rabbit/client/images/add.png"));
                floatingAddBtn.setGraphic(new ImageView(addIcon) {{
                    setFitWidth(55);
                    setFitHeight(55);
                }});
            } catch (Exception e) {
                floatingAddBtn.setText("+");
            }

            floatingAddBtn.setOnAction(e -> openCreateTaskPopup());
            topCreateTaskBtn.setOnAction(e -> openCreateTaskPopup());
        }
    }

    @FXML
    private void handleCreateTask() {
        openCreateTaskPopup();
    }

    private void openCreateTaskPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/rabbit/client/fxml/create-task-popup.fxml")
            );
            Pane overlay = loader.load();

            CreateTaskPopupController controller = loader.getController();
            controller.setup(currentProjectId, this::loadTasksFromServer);

            rootStackPane.getChildren().add(overlay);

            overlay.prefWidthProperty().bind(rootStackPane.widthProperty());
            overlay.prefHeightProperty().bind(rootStackPane.heightProperty());

        } catch (IOException e) {
        }
    }

    private void createColumns() {
        boardHBox.getChildren().clear();
        for (TaskStatus status : TaskStatus.values()) {
            VBox column = new VBox(15);
            column.getStyleClass().add("column-container");

            Label header = new Label(status.getValue().toUpperCase().replace("_", " "));
            header.getStyleClass().add("column-header-label");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);

            VBox tasksList = new VBox(12);
            VBox.setVgrow(tasksList, Priority.ALWAYS);
            tasksList.setMinWidth(280);

            setupDropTarget(tasksList, status);

            column.getChildren().addAll(header, tasksList);
            boardHBox.getChildren().add(column);
            columnContainers.put(status, tasksList);
        }
    }

    private void loadTasksFromServer() {
        new Thread(() -> {
            try {
                var response = apiClient.get("/tasks/" + currentProjectId);
                if (apiClient.isSuccess(response)) {
                    List<TaskDto> tasks = mapper.readValue(response.body(), new TypeReference<>() {});
                    Platform.runLater(() -> {
                        columnContainers.values().forEach(v -> v.getChildren().clear());
                        for (TaskDto task : tasks) {
                            TaskStatus status = TaskStatus.fromValue(task.getStatus());
                            if (columnContainers.containsKey(status)) {
                                TaskCardComponent card = new TaskCardComponent(task);
                                card.setAccessibleText(String.valueOf(task.getId()));
                                setupDragSource(card, task);
                                setupDragSource(card, task);
                                columnContainers.get(status).getChildren().add(card);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void setupDragSource(TaskCardComponent card, TaskDto task) {
        card.setOnDragDetected(event -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(task.getId()));
            db.setContent(content);

            SnapshotParameters params = new SnapshotParameters();
            params.setFill(Color.TRANSPARENT);
            WritableImage snapshot = card.snapshot(params, null);
            db.setDragView(snapshot);
            db.setDragViewOffsetX(event.getX());
            db.setDragViewOffsetY(event.getY());

            card.setOpacity(0.3);
            event.consume();
        });

        card.setOnDragDone(event -> {
            card.setOpacity(1.0);
            event.consume();
        });
    }

    private void setupDropTarget(VBox target, TaskStatus targetStatus) {
        target.setOnDragOver(event -> {
            if (event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        target.setOnDragEntered(event -> {
            if (event.getDragboard().hasString()) {
                target.getStyleClass().add("column-drag-entered");
            }
        });

        target.setOnDragExited(event -> {
            target.getStyleClass().remove("column-drag-entered");
        });

        target.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString()) {
                int taskId = Integer.parseInt(db.getString());
                TaskCardComponent draggedCard = findCardInUi(taskId);

                if (draggedCard != null) {
                    double mouseX = event.getSceneX();
                    double mouseY = event.getSceneY();

                    int targetIndex = 0;
                    for (int i = 0; i < target.getChildren().size(); i++) {
                        javafx.scene.Node node = target.getChildren().get(i);
                        if (event.getY() < (node.getLayoutY() + node.getBoundsInParent().getHeight() / 2)) {
                            break;
                        }
                        targetIndex++;
                    }

                    final int finalIndex = targetIndex;
                    Platform.runLater(() -> {
                        if (draggedCard.getParent() != null) {
                            ((Pane) draggedCard.getParent()).getChildren().remove(draggedCard);
                        }
                        if (finalIndex >= target.getChildren().size()) {
                            target.getChildren().add(draggedCard);
                        } else {
                            target.getChildren().add(finalIndex, draggedCard);
                        }

                        if (targetStatus == TaskStatus.DONE) {
                            javafx.geometry.Point2D localPoint = rootStackPane.sceneToLocal(mouseX, mouseY);
                            triggerConfetti(localPoint.getX(), localPoint.getY());
                        }
                    });

                    updateTaskStatusRequest(draggedCard.getTask(), targetStatus);
                    success = true;
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private TaskCardComponent findCardInUi(int taskId) {
        String idStr = String.valueOf(taskId);
        for (VBox column : columnContainers.values()) {
            for (javafx.scene.Node node : column.getChildren()) {
                if (node instanceof TaskCardComponent card) {
                    if (idStr.equals(card.getAccessibleText())) {
                        return card;
                    }
                }
            }
        }
        return null;
    }

    private void updateTaskStatusRequest(TaskDto task, TaskStatus newStatus) {
        new Thread(() -> {
            try {
                Map<String, String> payload = new HashMap<>();
                payload.put("status", newStatus.getValue());

                String json = mapper.writeValueAsString(payload);
                var response = apiClient.put("/tasks/" + task.getId() + "/status", json);

                if (apiClient.isSuccess(response)) {
                    task.setStatus(newStatus.getValue());
                } else {
                    if (response.statusCode() == 403) {
                        showNotification("Access Denied: You cannot move this task", Color.web("#ED4245"));
                    } else {
                        showNotification("Error: " + response.statusCode(), Color.web("#ED4245"));
                    }

                    Platform.runLater(this::loadTasksFromServer);
                }
            } catch (Exception e) {
                Platform.runLater(this::loadTasksFromServer);
            }
        }).start();
    }

    private void triggerConfetti(double startX, double startY) {
        int particlesCount = 100;
        Color[] colors = {Color.web("#8AB0C2"), Color.web("#61899B"), Color.WHITE, Color.GOLD, Color.PINK};
        Random random = new Random();

        for (int i = 0; i < particlesCount; i++) {
            Shape particle;
            if (random.nextBoolean()) {
                particle = new Circle(random.nextInt(4) + 2, colors[random.nextInt(colors.length)]);
            } else {
                particle = new Rectangle(random.nextInt(5) + 3, random.nextInt(5) + 3, colors[random.nextInt(colors.length)]);
            }

            particle.setManaged(false);
            particle.setMouseTransparent(true);
            particle.setLayoutX(startX);
            particle.setLayoutY(startY);

            rootStackPane.getChildren().add(particle);

            double angle = random.nextDouble() * 360;
            double distance = 50 + random.nextDouble() * 300;
            double endX = Math.cos(Math.toRadians(angle)) * distance;
            double endY = Math.sin(Math.toRadians(angle)) * distance;

            TranslateTransition move = new TranslateTransition(Duration.seconds(1.0 + random.nextDouble() * 0.5), particle);
            move.setByX(endX);
            move.setByY(endY);

            RotateTransition rotate = new RotateTransition(Duration.seconds(1.5), particle);
            rotate.setByAngle(random.nextInt(720) - 360);

            FadeTransition fade = new FadeTransition(Duration.seconds(1.5), particle);
            fade.setToValue(0);

            ParallelTransition pt = new ParallelTransition(particle, move, rotate, fade);
            pt.setOnFinished(e -> rootStackPane.getChildren().remove(particle));
            pt.play();
        }
    }

    private void showNotification(String message, Color color) {
        MainController currentMain = MainController.getInstance();
        if (currentMain != null) {
            currentMain.showGlobalNotification(message, toHexString(color));
        }
    }

    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed()   * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue()  * 255));
    }
}