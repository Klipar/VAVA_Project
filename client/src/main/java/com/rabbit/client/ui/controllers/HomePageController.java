package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.client.Config;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HomePageController {

    @FXML private FlowPane projectsContainer;
    @FXML private VBox tasksSection;
    @FXML private TableView<TaskDto> tasksTable;
    @FXML private TableColumn<TaskDto, String> colTaskName, colProject, colPriority, colDueDate;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
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

        colPriority.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    int level = getTableRow().getItem().getPriority();
                    var p = com.rabbit.common.enums.Priority.fromLevel(level);
                    setText(p.name());
                    setStyle("-fx-text-fill: " + p.getColor() + "; -fx-font-weight: bold;");
                }
            }
        });

        // ВИПРАВЛЕНИЙ ФОРМАТ ДАТИ В ТАБЛИЦІ
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
                    String rawDeadline = getTableRow().getItem().getDeadline();
                    String formattedDate = Config.getInstance().getBundle().getString("no_deadline");

                    if (rawDeadline != null && !rawDeadline.isEmpty()) {
                        try {
                            // Сервер присилає LocalDateTime.toString(), парсимо його
                            java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(rawDeadline);
                            formattedDate = ldt.format(Config.getInstance().getDateTimeFormatter()).toUpperCase();
                        } catch (Exception e) {
                            formattedDate = rawDeadline.toUpperCase();
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
                var currentUser = UserService.getInstance().getCurrentUser();
                if (currentUser == null) return;
                long myId = currentUser.getId();

                HttpResponse<String> projResp = apiClient.get("/projects");
                if (projResp.statusCode() != 200) return;

                allProjects = mapper.readValue(projResp.body(), new TypeReference<>() {});

                List<TaskDto> myTasks = new ArrayList<>();

                for (ProjectDto project : allProjects) {
                    HttpResponse<String> taskResp = apiClient.get("/tasks/" + project.getId());
                    if (taskResp.statusCode() == 200) {
                        List<TaskDto> projectTasks = mapper.readValue(taskResp.body(), new TypeReference<>() {});

                        for (TaskDto t : projectTasks) {
                            if (t.getAssignedTo() == (int) myId) {
                                t.setProjectId(project.getId());
                                myTasks.add(t);
                            }
                        }
                    }
                }
                
                myTasks.sort((t1, t2) -> {
                    if (t1.getDeadline() == null && t2.getDeadline() == null) return 0;
                    if (t1.getDeadline() == null) return 1;
                    if (t2.getDeadline() == null) return -1;
                    return t1.getDeadline().compareTo(t2.getDeadline());
                });

                Platform.runLater(() -> {
                    projectsContainer.getChildren().clear();
                    allProjects.forEach(this::addProjectCard);
                    tasksTable.setItems(javafx.collections.FXCollections.observableArrayList(myTasks));
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
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

        Hyperlink openTasks = new Hyperlink(Config.getInstance().getBundle().getString("view_open_tasks"));
        openTasks.getStyleClass().add("project-link");
        openTasks.setOnAction(e -> navigateToProjectBoard(project));

        card.getChildren().addAll(header, new Region() {{ VBox.setVgrow(this, Priority.ALWAYS); }}, openTasks);
        projectsContainer.getChildren().add(card);
    }

    private void navigateToProjectBoard(ProjectDto project) {
        if (mainController != null) {
            mainController.loadView("board-page.fxml", project.getId(), project.getTitle());
        }
    }

    private void handleTaskClick(TaskDto task) {
        allProjects.stream()
                .filter(p -> (long) p.getId() == task.getProjectId())
                .findFirst()
                .ifPresent(this::navigateToProjectBoard);
    }
}