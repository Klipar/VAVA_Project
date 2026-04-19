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
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
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

    private List<ProjectDto> allProjects = new ArrayList<>();

    @FXML
    public void initialize() {
        setupTableColumns();
        setupTableSelection(); // Додано для обробки кліків по таблиці
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
            Optional<ProjectDto> project = allProjects.stream()
                    .filter(p -> (long) p.getId() == projectId)
                    .findFirst();

            String title = project.map(ProjectDto::getTitle).orElse("UNKNOWN");
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
                    String rawDate = getTableRow().getItem().getDeadline();
                    if (rawDate != null && !rawDate.isEmpty()) {
                        try {
                            java.time.temporal.TemporalAccessor temporal;
                            if (rawDate.contains("Z") || rawDate.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                                temporal = java.time.OffsetDateTime.parse(rawDate);
                            } else {
                                temporal = java.time.LocalDateTime.parse(rawDate);
                            }
                            dateLabel.setText(displayFormatter.format(temporal).toUpperCase());
                        } catch (Exception e) {
                            dateLabel.setText(rawDate.split("\\.")[0].replace("T", " ").toUpperCase());
                        }
                    } else {
                        dateLabel.setText("NO DEADLINE");
                    }
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
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    TaskDto task = row.getItem();
                    handleTaskClick(task);
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

                if (tasksSection.isVisible()) {
                    long currentUserId = UserService.getInstance().getCurrentUser().getId();
                    List<TaskDto> allMyTasks = new ArrayList<>();

                    for (ProjectDto project : allProjects) {
                        HttpResponse<String> taskResp = apiClient.get("/tasks/" + project.getId());
                        if (taskResp.statusCode() == 200) {
                            List<TaskDto> projectTasks = mapper.readValue(taskResp.body(), new TypeReference<>() {});
                            List<TaskDto> myTasks = projectTasks.stream()
                                    .filter(t -> t.getAssignedTo() == currentUserId)
                                    .toList();
                            allMyTasks.addAll(myTasks);
                        }
                    }

                    allMyTasks.sort(java.util.Comparator.comparing(TaskDto::getDeadline,
                            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));

                    Platform.runLater(() -> tasksTable.getItems().setAll(allMyTasks));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addProjectCard(ProjectDto project) {
        VBox card = new VBox(10);
        card.getStyleClass().add("project-card");
        card.setPrefSize(240, 140);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(project.getTitle().toUpperCase());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(title, spacer);

        var starStream = getClass().getResourceAsStream("/com/rabbit/client/images/star.png");
        if (starStream != null) {
            ImageView star = new ImageView(new Image(starStream));
            star.setFitWidth(18); star.setFitHeight(18);
            header.getChildren().add(star);
        }

        Hyperlink openTasks = new Hyperlink("VIEW OPEN TASKS");
        Hyperlink assignedTasks = new Hyperlink("VIEW ASSIGNED TASKS");
        openTasks.getStyleClass().add("project-link");
        assignedTasks.getStyleClass().add("project-link");

        // Додаємо обробники для посилань у картці
        openTasks.setOnAction(e -> handleViewOpenTasks(project));
        assignedTasks.setOnAction(e -> handleViewAssignedTasks(project));

        Region bottomSpacer = new Region();
        VBox.setVgrow(bottomSpacer, Priority.ALWAYS);

        card.getChildren().addAll(header, bottomSpacer, openTasks, assignedTasks);
        projectsContainer.getChildren().add(card);
    }

    // --- МЕТОДИ ДЛЯ ОБРОБКИ НАТИСКАНЬ (TODO) ---

    private void handleViewOpenTasks(ProjectDto project) {
        // TODO: Реалізувати логіку переходу до списку відкритих завдань проекту
        System.out.println("View Open Tasks clicked for project: " + project.getTitle());
    }

    private void handleViewAssignedTasks(ProjectDto project) {
        // TODO: Реалізувати логіку переходу до списку призначених завдань проекту
        System.out.println("View Assigned Tasks clicked for project: " + project.getTitle());
    }

    private void handleTaskClick(TaskDto task) {
        // TODO: Реалізувати логіку відкриття деталей завдання (наприклад, модальне вікно або зміна вкладки)
        System.out.println("Task row clicked: " + task.getTitle());
    }
}