package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.service.ApiClient;
import com.rabbit.client.service.UserService;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.enums.UserRole;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Setter;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class HomePageController {

    @FXML private FlowPane projectsContainer;
    @FXML private VBox tasksSection;
    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> colTaskName, colProject, colPriority, colDueDate;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy, HH:mm", Locale.ENGLISH);

    @Setter
    private MainController mainController;

    private List<ProjectDto> allProjects = new ArrayList<>();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableSelection();
        checkPermissions();
        loadData();
    }

    private void checkPermissions() {
        UserRole role = UserService.getInstance().getCurrentUser().getRole();
        if (role == UserRole.MANAGER) {
            tasksSection.setVisible(false);
            tasksSection.setManaged(false);
        }
    }

    private void setupTableColumns() {
        colTaskName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle().toUpperCase()));
        colProject.setCellValueFactory(data -> {
            long projectId = data.getValue().getProjectId();
            String title = allProjects.stream()
                    .filter(p -> (long) p.getId() == projectId)
                    .map(ProjectDto::getTitle)
                    .findFirst().orElse("UNKNOWN");
            return new SimpleStringProperty(title.toUpperCase());
        });
        colPriority.setCellValueFactory(data -> {
            int p = data.getValue().getPriority();
            return new SimpleStringProperty(p >= 3 ? "HIGH" : (p == 2 ? "MEDIUM" : "LOW"));
        });
        colDueDate.setCellFactory(column -> new TableCell<>() {
            private final HBox container = new HBox(8);
            private final Label dateLabel = new Label();
            private ImageView warningIcon;
            {
                container.setAlignment(Pos.CENTER_LEFT);
                var stream = getClass().getResourceAsStream("/com/rabbit/client/images/worning.png");
                if (stream != null) {
                    warningIcon = new ImageView(new Image(stream));
                    warningIcon.setFitWidth(16); warningIcon.setFitHeight(16);
                }
                dateLabel.setStyle("-fx-text-fill: #99AAB5;");
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    dateLabel.setText(getTableRow().getItem().getDeadline()); // Simplified for brevity
                    container.getChildren().clear();
                    if (warningIcon != null) container.getChildren().add(warningIcon);
                    container.getChildren().add(dateLabel);
                    setGraphic(container);
                }
            }
        });
    }

    private void setupTableSelection() {
        tasksTable.setRowFactory(tv -> {
            TableRow<TaskDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleTaskClick(row.getItem());
                }
            });
            return row;
        });
    }

    private void loadData() {
        new Thread(() -> {
            try {
                HttpResponse<String> projResp = apiClient.get("/projects");
                if (projResp.statusCode() != 200) return;
                allProjects = mapper.readValue(projResp.body(), new TypeReference<>() {});

                Platform.runLater(() -> {
                    projectsContainer.getChildren().clear();
                    allProjects.forEach(this::addProjectCard);
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void addProjectCard(ProjectDto project) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPrefSize(240, 140);
        card.setCursor(javafx.scene.Cursor.HAND);

        card.setOnMouseClicked(e -> navigateToProjectBoard(project));

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(project.getTitle().toUpperCase());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        var starStream = getClass().getResourceAsStream("/com/rabbit/client/images/star.png");
        if (starStream != null) {
            header.getChildren().add(new ImageView(new Image(starStream)) {{ setFitWidth(18); setFitHeight(18); }});
        }

        Hyperlink openTasks = new Hyperlink("VIEW OPEN TASKS");
        openTasks.getStyleClass().add("project-link");
        openTasks.setOnAction(e -> navigateToProjectBoard(project));

        card.getChildren().addAll(header, new Region() {{ VBox.setVgrow(this, Priority.ALWAYS); }}, openTasks);
        projectsContainer.getChildren().add(card);
    }

    private void navigateToProjectBoard(ProjectDto project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/rabbit/client/fxml/board-page.fxml"));

            // Створюємо контролер вручну (як ти і робив, бо так у тебе працює таблиця)
            BoardController boardController = new BoardController();

            // 1. Встановлюємо дані проекту
            boardController.setCurrentProject(project.getId(), project.getTitle());

            // 2. КРИТИЧНО: Передаємо посилання на MainController
            // Саме через це раніше писало "MainController not set"
            boardController.setMainController(this.mainController);

            // Прив'язуємо цей конкретний екземпляр до завантажувача
            loader.setController(boardController);

            Parent boardView = loader.load();

            if (mainController != null) {
                mainController.setView(boardView);
            }
        } catch (IOException e) {
            System.err.println("Помилка завантаження дошки проекту: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleTaskClick(TaskDto task) {
        allProjects.stream()
                .filter(p -> (long) p.getId() == task.getProjectId())
                .findFirst()
                .ifPresent(this::navigateToProjectBoard);
    }
}