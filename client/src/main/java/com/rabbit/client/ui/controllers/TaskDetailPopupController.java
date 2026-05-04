package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.Priority;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TaskDetailPopupController {

    // Common
    @FXML private StackPane overlayPane;
    @FXML private VBox popupCard;
    @FXML private Label taskTitleLabel;
    @FXML private Button editBtn;

    // View mode
    @FXML private VBox viewModeBox;
    @FXML private TextArea viewDescription;
    @FXML private Label viewStatus;
    @FXML private Label viewPriority;
    @FXML private Label viewDeadline;
    @FXML private Label viewReporter;
    @FXML private Label viewAssignee;

    // Edit mode
    @FXML private VBox editModeBox;
    @FXML private TextField editTitleField;
    @FXML private TextArea editDescriptionField;
    @FXML private DatePicker editDeadlinePicker;
    @FXML private Label editStatusLabel;
    @FXML private HBox editPriorityBox;
    @FXML private ComboBox<UserDto> editAssigneeCombo;
    @FXML private TextField editSkillsField;
    @FXML private Button editAiSuggestBtn;
    @FXML private ProgressIndicator editAiLoadingIndicator;
    @FXML private VBox editAiResultsBox;
    @FXML private Label editAiResultsTitle;
    @FXML private Button saveBtn;

    private static final String[] PRIORITY_LABELS = {"Trivial", "Low", "Medium", "High", "Critical"};
    private static final String[] PRIORITY_COLORS = {"#95a5a6", "#27ae60", "#f1c40f", "#e67e22", "#e74c3c"};

    private final ApiClient apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<Circle> priorityCircles = new ArrayList<>();

    private TaskDto currentTask;
    private boolean isMaster = false;
    private int currentProjectId;
    private int selectedPriority = -1;
    private Runnable onTaskChanged;

    @FXML
    public void initialize() {
        buildPriorityButtons();
        configureAssigneeCombo();
        playOpenAnimation();

        overlayPane.widthProperty().addListener((obs, oldV, newV) ->
                popupCard.setMaxWidth(Math.min(680, newV.doubleValue() * 0.85)));
        overlayPane.heightProperty().addListener((obs, oldV, newV) ->
                popupCard.setMaxHeight(Math.min(860, newV.doubleValue() * 0.9)));
    }

    public void setup(TaskDto task, boolean isMaster, int projectId, Runnable onTaskChanged) {
        this.currentTask = task;
        this.isMaster = isMaster;
        this.currentProjectId = projectId;
        this.onTaskChanged = onTaskChanged;

        populateViewMode(task);

        editBtn.setVisible(isMaster);
        editBtn.setManaged(isMaster);

        if (isMaster) {
            loadProjectUsers();
        }
    }

    private void populateViewMode(TaskDto task) {
        taskTitleLabel.setText(task.getTitle() != null ? task.getTitle() : "Task");

        viewDescription.setText(task.getDescription() != null ? task.getDescription() : "");

        String status = task.getStatus();
        viewStatus.setText(status != null ? status.toUpperCase().replace("_", " ") : "");

        Priority p = Priority.fromLevel(task.getPriority());
        viewPriority.setText(p.name());
        viewPriority.setStyle("-fx-text-fill: " + p.getColor() + "; -fx-font-weight: bold; -fx-font-size: 13px;");

        viewDeadline.setText(formatDeadline(task.getDeadline()));

        // Reporter and Assignee - load async
        loadUserName(task.getCreatedBy(), viewReporter);
        loadUserName(task.getAssignedTo(), viewAssignee);
    }

    private void populateEditMode(TaskDto task) {
        editTitleField.setText(task.getTitle() != null ? task.getTitle() : "");
        editDescriptionField.setText(task.getDescription() != null ? task.getDescription() : "");

        String status = task.getStatus();
        editStatusLabel.setText(status != null ? status.toUpperCase().replace("_", " ") : "");

        // Set priority
        selectPriority(task.getPriority(), PRIORITY_COLORS[Math.min(task.getPriority(), PRIORITY_COLORS.length - 1)]);

        // Set deadline
        if (task.getDeadline() != null && !task.getDeadline().isBlank() && !"null".equalsIgnoreCase(task.getDeadline())) {
            try {
                String dl = task.getDeadline();
                if (!dl.endsWith("Z") && !dl.contains("+")) dl += "Z";
                ZonedDateTime zdt = ZonedDateTime.parse(dl);
                editDeadlinePicker.setValue(zdt.toLocalDate());
            } catch (Exception ignored) {}
        }
    }

    private void loadUserName(int userId, Label target) {
        if (userId <= 0) {
            target.setText("—");
            return;
        }
        new Thread(() -> {
            try {
                var resp = apiClient.get("/users/" + userId);
                if (apiClient.isSuccess(resp)) {
                    UserDto user = mapper.readValue(resp.body(), UserDto.class);
                    String display = user.getNickname() != null ? user.getNickname() :
                            (user.getName() != null ? user.getName() : "User #" + userId);
                    // Shorten to "First L." format if possible
                    String name = user.getName() != null ? user.getName() : display;
                    String[] parts = name.split(" ");
                    String short_name = parts.length >= 2 ?
                            parts[0] + " " + parts[1].charAt(0) + "." : name;
                    Platform.runLater(() -> target.setText(short_name));
                } else {
                    Platform.runLater(() -> target.setText("User #" + userId));
                }
            } catch (Exception e) {
                Platform.runLater(() -> target.setText("User #" + userId));
            }
        }).start();
    }

    private void loadProjectUsers() {
        new Thread(() -> {
            try {
                var resp = apiClient.get("/projects/" + currentProjectId + "/users");
                if (apiClient.isSuccess(resp)) {
                    List<UserDto> users = mapper.readValue(resp.body(), new TypeReference<>() {});
                    Platform.runLater(() -> {
                        editAssigneeCombo.getItems().setAll(users);
                        // Pre-select current assignee
                        users.stream()
                                .filter(u -> u.getId() != null && u.getId().intValue() == currentTask.getAssignedTo())
                                .findFirst()
                                .ifPresent(u -> editAssigneeCombo.setValue(u));
                    });
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void buildPriorityButtons() {
        for (int i = 0; i < PRIORITY_LABELS.length; i++) {
            final int value = i;
            final String color = PRIORITY_COLORS[i];

            Circle circle = new Circle(14);
            circle.setFill(Color.web("#1a3a56"));
            circle.setStroke(Color.web(color));
            circle.setStrokeWidth(2.5);
            priorityCircles.add(circle);

            Button btn = new Button();
            btn.setGraphic(circle);
            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            btn.setOnAction(e -> selectPriority(value, color));

            Label lbl = new Label(PRIORITY_LABELS[i]);
            lbl.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 9px;");

            VBox wrapper = new VBox(3, btn, lbl);
            wrapper.setAlignment(Pos.CENTER);
            editPriorityBox.getChildren().add(wrapper);
        }
    }

    private void selectPriority(int value, String color) {
        int safeValue = Math.max(0, Math.min(value, priorityCircles.size() - 1));
        priorityCircles.forEach(c -> c.setFill(Color.web("#1a3a56")));
        priorityCircles.get(safeValue).setFill(Color.web(PRIORITY_COLORS[safeValue]));
        selectedPriority = safeValue;
    }

    private void configureAssigneeCombo() {
        editAssigneeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(UserDto u) {
                return u == null ? "" : (u.getNickname() != null ? u.getNickname() : u.getName()) +
                        " (" + (u.getName() != null ? u.getName() : "") + ")";
            }
            @Override public UserDto fromString(String s) { return null; }
        });
    }

    @FXML
    private void handleEdit() {
        populateEditMode(currentTask);
        viewModeBox.setVisible(false);
        viewModeBox.setManaged(false);
        editModeBox.setVisible(true);
        editModeBox.setManaged(true);
        editBtn.setVisible(false);
        editBtn.setManaged(false);
    }

    @FXML
    private void handleCancelEdit() {
        editModeBox.setVisible(false);
        editModeBox.setManaged(false);
        viewModeBox.setVisible(true);
        viewModeBox.setManaged(true);
        editBtn.setVisible(true);
        editBtn.setManaged(true);
        clearAiResults();
    }

    @FXML
    private void handleSaveChanges() {
        UserDto assignee = editAssigneeCombo.getValue();
        if (assignee == null || selectedPriority == -1) {
            showError("Please fill all fields and select priority.");
            return;
        }
        if (editTitleField.getText().isBlank()) {
            showError("Task name cannot be empty.");
            return;
        }

        saveBtn.setDisable(true);
        saveBtn.setText("Saving...");

        String deadline = null;
        if (editDeadlinePicker.getValue() != null) {
            deadline = LocalDateTime.of(editDeadlinePicker.getValue(), LocalTime.of(23, 59))
                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", editTitleField.getText().trim());
        payload.put("description", editDescriptionField.getText().trim());
        payload.put("deadline", deadline);
        payload.put("priority", selectedPriority);
        payload.put("assignedTo", assignee.getId());
        payload.put("status", currentTask.getStatus());

        final String finalDeadline = deadline;

        new Thread(() -> {
            try {
                var resp = apiClient.put("/tasks/" + currentTask.getId() + "/update",
                        mapper.writeValueAsString(payload));
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(resp)) {
                        // Update local task object
                        currentTask.setTitle(editTitleField.getText().trim());
                        currentTask.setDescription(editDescriptionField.getText().trim());
                        currentTask.setPriority(selectedPriority);
                        currentTask.setDeadline(finalDeadline);
                        currentTask.setAssignedTo(assignee.getId().intValue());

                        if (onTaskChanged != null) onTaskChanged.run();
                        handleCancelEdit();

                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification("Task updated successfully!", "#6aa896");
                    } else {
                        saveBtn.setDisable(false);
                        saveBtn.setText("Save changes");
                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification("Failed to update: " + resp.statusCode(), "#ED4245");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    saveBtn.setDisable(false);
                    saveBtn.setText("Save changes");
                });
            }
        }).start();
    }

    @FXML
    private void handleDeleteTask() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Delete this task?");
        alert.setContentText("Are you sure you want to delete \"" + currentTask.getTitle() + "\"?");
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        var resp = apiClient.delete("/tasks/" + currentTask.getId() + "/delete");
                        Platform.runLater(() -> {
                            if (apiClient.isSuccess(resp)) {
                                if (onTaskChanged != null) onTaskChanged.run();
                                closePopup();
                                MainController mc = MainController.getInstance();
                                if (mc != null) mc.showGlobalNotification("Task deleted.", "#6aa896");
                            } else {
                                MainController mc = MainController.getInstance();
                                if (mc != null) mc.showGlobalNotification("Delete failed: " + resp.statusCode(), "#ED4245");
                            }
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            MainController mc = MainController.getInstance();
                            if (mc != null) mc.showGlobalNotification("Error deleting task.", "#ED4245");
                        });
                    }
                }).start();
            }
        });
    }

    @FXML
    private void handleAiSuggest() {
        String skills = editSkillsField.getText().trim();
        if (skills.isBlank()) {
            showAiError(Config.getInstance().getBundle().getString("enter_skill"));
            return;
        }

        setAiLoading(true);
        clearAiResults();

        new Thread(() -> {
            try {
                var usersResp = apiClient.get("/projects/" + currentProjectId + "/users");
                if (!apiClient.isSuccess(usersResp)) {
                    Platform.runLater(() -> setAiLoading(false));
                    return;
                }

                List<UserDto> users = mapper.readValue(usersResp.body(), new TypeReference<>() {});
                List<String> requiredSkills = Arrays.stream(skills.split(","))
                        .map(String::trim).filter(s -> !s.isBlank()).toList();

                List<Map<String, Object>> workers = users.stream().map(u -> {
                    Map<String, Object> w = new HashMap<>();
                    w.put("id", u.getId());
                    w.put("name", u.getNickname() != null ? u.getNickname() : u.getName());
                    w.put("skills", List.of());
                    w.put("active_tasks", 0);
                    w.put("max_tasks", 5);
                    w.put("past_tasks", List.of());
                    return w;
                }).toList();

                Map<String, Object> body = Map.of(
                        "description", editDescriptionField.getText().trim(),
                        "required_skills", requiredSkills,
                        "workers", workers
                );

                var aiResp = apiClient.post("/ai/suggest", mapper.writeValueAsString(body));
                if (apiClient.isSuccess(aiResp)) {
                    Map<String, Object> result = mapper.readValue(aiResp.body(), new TypeReference<>() {});
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> top3 =
                            ((List<Map<String, Object>>) result.get("ranked_workers")).stream().limit(3).toList();
                    Platform.runLater(() -> { setAiLoading(false); renderAiResults(top3, users); });
                } else {
                    Platform.runLater(() -> { setAiLoading(false); showAiError("AI error: " + aiResp.statusCode()); });
                }
            } catch (Exception e) {
                Platform.runLater(() -> { setAiLoading(false); showAiError("Failed to connect to AI service."); });
            }
        }).start();
    }

    private void renderAiResults(List<Map<String, Object>> ranked, List<UserDto> users) {
        Map<Long, UserDto> userMap = new HashMap<>();
        users.forEach(u -> userMap.put(u.getId(), u));

        for (int i = 0; i < ranked.size(); i++) {
            var entry = ranked.get(i);
            long id = ((Number) entry.get("worker_id")).longValue();
            String expl = (String) entry.getOrDefault("explanation", "");
            UserDto target = userMap.get(id);
            String name = target != null ?
                    (target.getNickname() != null ? target.getNickname() : target.getName()) :
                    (String) entry.getOrDefault("worker_name", "Unknown");

            Label rankLbl = new Label((i + 1) + ".");
            rankLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;-fx-min-width:20;");
            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;");
            HBox.setHgrow(nameLbl, javafx.scene.layout.Priority.ALWAYS);
            Label avatar = new Label("\uD83D\uDC64");
            avatar.setStyle("-fx-font-size:18px;-fx-opacity:0.7;");

            HBox left = new HBox(10, rankLbl, nameLbl, avatar);
            left.setAlignment(Pos.CENTER_LEFT);
            left.setPadding(new Insets(12, 14, 12, 14));
            left.setStyle("-fx-background-color:#0a1f33;-fx-background-radius:9 0 0 9;-fx-min-width:160;-fx-cursor:hand;");

            if (target != null) {
                final UserDto tgt = target;
                left.setOnMouseClicked(e -> editAssigneeCombo.setValue(tgt));
                left.setOnMouseEntered(e -> left.setStyle("-fx-background-color:#1a3a56;-fx-background-radius:9 0 0 9;-fx-min-width:160;-fx-cursor:hand;"));
                left.setOnMouseExited(e -> left.setStyle("-fx-background-color:#0a1f33;-fx-background-radius:9 0 0 9;-fx-min-width:160;-fx-cursor:hand;"));
            }

            Label explLbl = new Label("• " + expl);
            explLbl.setStyle("-fx-text-fill:#333;-fx-font-size:12px;");
            explLbl.setWrapText(true);
            HBox.setHgrow(explLbl, javafx.scene.layout.Priority.ALWAYS);

            HBox right = new HBox(explLbl);
            right.setAlignment(Pos.CENTER_LEFT);
            right.setPadding(new Insets(12, 14, 12, 14));
            right.setStyle("-fx-background-color:white;-fx-background-radius:0 9 9 0;");
            HBox.setHgrow(right, javafx.scene.layout.Priority.ALWAYS);

            HBox row = new HBox(left, right);
            row.setStyle("-fx-border-color:#3E6273;-fx-border-radius:10;-fx-border-width:2;-fx-background-radius:10;");
            VBox.setMargin(row, new Insets(i == 0 ? 4 : 8, 0, 0, 0));

            editAiResultsBox.getChildren().add(row);
        }

        editAiResultsTitle.setVisible(true);
        editAiResultsTitle.setManaged(true);
        editAiResultsBox.setVisible(true);
        editAiResultsBox.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), editAiResultsBox);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void clearAiResults() {
        if (editAiResultsBox.getChildren().size() > 1)
            editAiResultsBox.getChildren().remove(1, editAiResultsBox.getChildren().size());
        editAiResultsBox.setVisible(false);
        editAiResultsBox.setManaged(false);
    }

    private void showAiError(String msg) {
        clearAiResults();
        editAiResultsTitle.setVisible(false);
        editAiResultsTitle.setManaged(false);
        Label err = new Label("⚠  " + msg);
        err.setStyle("-fx-text-fill:#e74c3c;-fx-font-size:12px;");
        VBox.setMargin(err, new Insets(4, 0, 0, 0));
        editAiResultsBox.getChildren().add(err);
        editAiResultsBox.setVisible(true);
        editAiResultsBox.setManaged(true);
    }

    private void setAiLoading(boolean loading) {
        editAiLoadingIndicator.setVisible(loading);
        editAiLoadingIndicator.setManaged(loading);
        editAiSuggestBtn.setDisable(loading);
    }

    private void showError(String msg) {
        MainController mc = MainController.getInstance();
        if (mc != null) mc.showGlobalNotification(msg, "#ED4245");
    }

    private String formatDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isBlank() || "null".equalsIgnoreCase(deadlineStr)) {
            return Config.getInstance().getBundle().getString("no_deadline");
        }
        try {
            if (!deadlineStr.endsWith("Z") && !deadlineStr.contains("+")) deadlineStr += "Z";
            ZonedDateTime zdt = ZonedDateTime.parse(deadlineStr);
            return zdt.format(Config.getInstance().getDateTimeFormatter()).toUpperCase();
        } catch (Exception e) {
            return deadlineStr.toUpperCase();
        }
    }

    @FXML private void handleClose() { closePopup(); }
    @FXML private void handleOverlayClick(MouseEvent e) { if (e.getTarget() == overlayPane) closePopup(); }
    @FXML private void consumeClick(MouseEvent e) { e.consume(); }

    private void closePopup() {
        FadeTransition fade = new FadeTransition(Duration.millis(160), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(160), popupCard);
        fade.setToValue(0); scale.setToX(0.85); scale.setToY(0.85);
        fade.setOnFinished(e -> {
            if (overlayPane.getParent() instanceof Pane p) p.getChildren().remove(overlayPane);
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