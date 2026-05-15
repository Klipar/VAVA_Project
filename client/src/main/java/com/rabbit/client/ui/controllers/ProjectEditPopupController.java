package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.common.dto.UserDto;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ProjectEditPopupController {

    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private Label projectTitleLabel;

    @FXML private TextField editTitleField;
    @FXML private TextArea editDescriptionField;
    @FXML private DatePicker editDeadlinePicker;

    @FXML private TextField assignField;
    @FXML private Button addAssignBtn;
    @FXML private ListView<String> suggestionsListView;
    @FXML private FlowPane assignedChipsPane;

    @FXML private Button cancelBtn;
    @FXML private Button deleteBtn;
    @FXML private Button saveBtn;

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ProjectDto currentProject;
    private Runnable onProjectChanged;

    private final List<String> assignedPeople = new ArrayList<>();
    private final List<UserDto> assignedUsers = new ArrayList<>();
    private List<UserDto> allUsers = new ArrayList<>();

    @FXML
    public void initialize() {
        playOpenAnimation();

        setupSuggestionsListView();
        setupAssignFieldListener();
        loadAllUsers();

        overlayPane.widthProperty().addListener((obs, oldV, newV) ->
                popupCard.setMaxWidth(Math.min(680, newV.doubleValue() * 0.85)));
        overlayPane.heightProperty().addListener((obs, oldV, newV) ->
                popupCard.setMaxHeight(Math.min(760, newV.doubleValue() * 0.9)));
    }

    public void setup(ProjectDto project, Runnable onProjectChanged) {
        this.currentProject = project;
        this.onProjectChanged = onProjectChanged;

        projectTitleLabel.setText(project.getTitle() != null ? project.getTitle() : "Edit Project");
        editTitleField.setText(project.getTitle() != null ? project.getTitle() : "");
        editDescriptionField.setText(project.getDescription() != null ? project.getDescription() : "");

        if (project.getDeadline() != null) {
            try {
                LocalDate ld = project.getDeadline().toLocalDate();
                editDeadlinePicker.setValue(ld);
            } catch (Exception ignored) {}
        }
        loadProjectUsers();
    }

    private void setupSuggestionsListView() {
        suggestionsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
            }
        });

        suggestionsListView.setOnMouseClicked(event -> {
            String selected = suggestionsListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                assignField.setText(selected);
                hideSuggestions();
                addUserByNickname(selected);
            }
        });
    }

    private void setupAssignFieldListener() {
        assignField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) {
                hideSuggestions();
                return;
            }
            String query = newVal.trim().toLowerCase();
            List<String> matches = allUsers.stream()
                .filter(u -> u.getNickname() != null && u.getNickname().toLowerCase().contains(query) && !isAlreadyAssigned(u))
                .map(UserDto::getNickname)
                .limit(8)
                .toList();

            if (matches.isEmpty()) {
                hideSuggestions();
            } else {
                suggestionsListView.getItems().setAll(matches);
                suggestionsListView.setPrefHeight(Math.min(matches.size() * 36, 180));
                showSuggestions();
            }
        });

        assignField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                new Thread(() -> {
                    try { Thread.sleep(150); } catch (InterruptedException ignored) {}
                    Platform.runLater(this::hideSuggestions);
                }).start();
            }
        });
    }

    private void showSuggestions() {
        suggestionsListView.setVisible(true);
        suggestionsListView.setManaged(true);
    }

    private void hideSuggestions() {
        suggestionsListView.setVisible(false);
        suggestionsListView.setManaged(false);
        suggestionsListView.getSelectionModel().clearSelection();
    }

    private boolean isAlreadyAssigned(UserDto user) {
        return assignedUsers.stream().anyMatch(u -> u.getId() != null && user.getId() != null && u.getId().longValue() == user.getId().longValue());
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                var resp = apiClient.get("/users/all");
                if (apiClient.isSuccess(resp)) {
                    List<UserDto> users = mapper.readValue(resp.body(), new TypeReference<>() {});
                    Platform.runLater(() -> allUsers = users);
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadProjectUsers() {
        new Thread(() -> {
            try {
                var resp = apiClient.get("/projects/" + currentProject.getId() + "/users");
                if (apiClient.isSuccess(resp)) {
                    List<UserDto> users = mapper.readValue(resp.body(), new TypeReference<>() {});
                    Platform.runLater(() -> {
                        for (UserDto u : users) {
                            addChip(u);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void addUserByNickname(String name) {
        var rb = Config.getInstance().getBundle();

        boolean alreadyAdded = assignedUsers.stream()
            .anyMatch(u -> u.getNickname() != null && u.getNickname().equalsIgnoreCase(name));
        if (alreadyAdded) {
            highlightAssignedChip(name);
            return;
        }

        UserDto currentUser = Config.getInstance().getUser();
        if (currentUser.getNickname() != null && currentUser.getNickname().equalsIgnoreCase(name)) {
            showError(rb.getString("creator_auto_added"));
            return;
        }
        UserDto cached = allUsers.stream()
            .filter(u -> u.getNickname() != null && u.getNickname().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);

        if (cached != null) {
            handleAddUserToProject(cached);
            assignField.clear();
            return;
        }
        new Thread(() -> {
            try {
                var resp = apiClient.get("/users/nickname/" + name);
                if (apiClient.isSuccess(resp)) {
                    UserDto fetched = mapper.readValue(resp.body(), UserDto.class);
                    Platform.runLater(() -> {
                        handleAddUserToProject(fetched);
                        assignField.clear();
                    });
                } else {
                    Platform.runLater(() -> showError(rb.getString("user_not_found") + ": " + name));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError(rb.getString("error_fetch_user")));
            }
        }).start();
    }

    private void handleAddUserToProject(UserDto user) {
        new Thread(() -> {
            try {
                var resp = apiClient.post("/projects/" + currentProject.getId() + "/users/" + user.getId() + "/add", "{}");
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(resp)) {
                            addChip(user);
                            MainController mc = MainController.getInstance();
                            if (mc != null) mc.showGlobalNotification(Config.getInstance().getBundle().getString("user_added_to_project"), "#6aa896");
                        } else {
                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification(Config.getInstance().getBundle().getString("update_failed") + ": " + resp.statusCode(), "#ED4245");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError(Config.getInstance().getBundle().getString("connection_error")));
            }
        }).start();
    }

    private void addChip(UserDto user) {
        if (user.getNickname() == null) return;
        assignedPeople.add(user.getNickname());
        assignedUsers.add(user);

        HBox chip = new HBox(5);
        chip.getStyleClass().add("assign-chip");
        Label nameLabel = new Label(user.getNickname());
        nameLabel.getStyleClass().add("assign-chip-label");

        Button removeBtn = new Button("✕");
        removeBtn.getStyleClass().add("assign-chip-remove");
        UserDto currentUser = Config.getInstance().getUser();
        if (currentUser.getId() != null && user.getId() != null && currentUser.getId().longValue() == user.getId().longValue()) {
            removeBtn.setOnAction(e -> {
                new Thread(() -> {
                    try {
                        var resp = apiClient.delete("/projects/" + currentProject.getId() + "/users/" + user.getId() + "/remove");
                        Platform.runLater(() -> {
                            if (apiClient.isSuccess(resp)) {
                                assignedPeople.remove(user.getNickname());
                                assignedUsers.removeIf(u -> u.getId() != null && user.getId() != null && u.getId().longValue() == user.getId().longValue());
                                assignedChipsPane.getChildren().remove(chip);
                            } else {
                                showError(Config.getInstance().getBundle().getString("update_failed") + ": " + resp.statusCode());
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> showError(Config.getInstance().getBundle().getString("connection_error")));
                    }
                }).start();
            });
        } else {
            removeBtn.setDisable(true);
            removeBtn.setTooltip(new Tooltip("Only users can remove themselves from a project"));
        }

        chip.getChildren().addAll(nameLabel, removeBtn);
        assignedChipsPane.getChildren().add(chip);
        HBox.setHgrow(chip, Priority.NEVER);
    }

    @FXML
    private void handleAddAssign() {
        String name = assignField.getText().trim();
        if (name.isBlank()) return;
        addUserByNickname(name);
    }

    private void highlightAssignedChip(String name) {
        for (Node node : assignedChipsPane.getChildren()) {
            if (node instanceof HBox chip) {
                for (Node child : chip.getChildren()) {
                    if (child instanceof Label lbl && lbl.getText() != null && lbl.getText().equalsIgnoreCase(name)) {
                        String oldStyle = chip.getStyle();
                        chip.setStyle("-fx-background-color: rgba(106,158,150,0.18); -fx-background-radius: 8;");
                        new Thread(() -> {
                            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> chip.setStyle(oldStyle));
                        }).start();
                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification(Config.getInstance().getBundle().getString("user_already_assigned") + ": " + name, "#EDC36A");
                        return;
                    }
                }
            }
        }
        showError(Config.getInstance().getBundle().getString("user_already_assigned") + ": " + name);
    }

    @FXML private void handleSaveChanges() {
        var rb = Config.getInstance().getBundle();
        if (editTitleField.getText().isBlank()) {
            showError(rb.getString("enter_project_name"));
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText(rb.getString("edit_user_save"));

        new Thread(() -> {
            try {
                ProjectDto dto = new ProjectDto();
                dto.setTitle(editTitleField.getText().trim());
                dto.setDescription(editDescriptionField.getText().trim());
                LocalDate dp = editDeadlinePicker.getValue();
                if (dp != null) dto.setDeadline(LocalDateTime.of(dp, LocalTime.of(23,59)));
                dto.setStatus(currentProject.getStatus());

                var resp = apiClient.put("/projects/" + currentProject.getId(), mapper.writeValueAsString(dto));
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(resp)) {
                            if (onProjectChanged != null) onProjectChanged.run();
                            closePopup();
                            MainController mc = MainController.getInstance();
                            if (mc != null) mc.showGlobalNotification(Config.getInstance().getBundle().getString("project_updated"), "#6aa896");
                        } else {
                        saveBtn.setDisable(false);
                        saveBtn.setText(rb.getString("edit_user_save"));
                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification(Config.getInstance().getBundle().getString("update_failed") + ": " + resp.statusCode(), "#ED4245");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> saveBtn.setDisable(false));
            }
        }).start();
    }

    @FXML private void handleDelete() {
        var rb = Config.getInstance().getBundle();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(rb.getString("delete_project_title"));
        alert.setHeaderText(rb.getString("delete_project_header") + " \"" + currentProject.getTitle() + "\"");
        alert.setContentText(rb.getString("delete_project_confirm"));
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        var resp = apiClient.delete("/projects/" + currentProject.getId());
                        Platform.runLater(() -> {
                            if (apiClient.isSuccess(resp)) {
                                if (onProjectChanged != null) onProjectChanged.run();
                                closePopup();
                                MainController mc = MainController.getInstance();
                                if (mc != null) mc.showGlobalNotification(rb.getString("project_deleted"), "#6aa896");
                            } else {
                                MainController mc = MainController.getInstance();
                                if (mc != null) mc.showGlobalNotification(rb.getString("delete_project_confirm") + ": " + resp.statusCode(), "#ED4245");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            MainController mc = MainController.getInstance();
                            if (mc != null) mc.showGlobalNotification(rb.getString("connection_error"), "#ED4245");
                        });
                    }
                }).start();
            }
        });
    }

    @FXML private void handleCancel() { closePopup(); }
    @FXML private void handleClose() { closePopup(); }
    @FXML private void handleOverlayClick(MouseEvent e) { if (e.getTarget() == overlayPane) closePopup(); }
    @FXML private void consumeClick(MouseEvent e) { e.consume(); }

    private void showError(String msg) {
        MainController mc = MainController.getInstance();
        if (mc != null) mc.showGlobalNotification(msg, "#ED4245");
    }

    private void closePopup() {
        FadeTransition fade = new FadeTransition(Duration.millis(160), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(160), popupCard);
        fade.setToValue(0); scale.setToX(0.85); scale.setToY(0.85);
        fade.setOnFinished(e -> {
            if (overlayPane.getParent() instanceof javafx.scene.layout.Pane p) p.getChildren().remove(overlayPane);
        });
        fade.play(); scale.play();
    }

    private void playOpenAnimation() {
        overlayPane.setOpacity(0); popupCard.setScaleX(0.85); popupCard.setScaleY(0.85);
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(220), popupCard);
        fade.setToValue(1); scale.setToX(1); scale.setToY(1);
        fade.play(); scale.play();
    }
}
