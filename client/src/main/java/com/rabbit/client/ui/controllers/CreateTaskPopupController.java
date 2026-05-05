package com.rabbit.client.ui.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.service.ApiClient;
import com.rabbit.common.dto.UserDto;
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
import javafx.scene.Cursor;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;
// modal imports removed; using inline expansion instead

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CreateTaskPopupController {

    @FXML private StackPane overlayPane;
    @FXML private VBox      popupCard;
    @FXML private TextField titleField;
    @FXML private TextArea  descriptionField;
    @FXML private DatePicker deadlinePicker;
    @FXML private HBox      priorityBox;
    @FXML private ComboBox<UserDto> assigneeCombo;
    @FXML private TextField skillsField;
    @FXML private Button    aiSuggestBtn;
    @FXML private ProgressIndicator aiLoadingIndicator;
    @FXML private VBox      aiResultsBox;
    @FXML private Label     aiResultsTitle;
    @FXML private Button    createBtn;

    private static final String[] PRIORITY_LABELS = {"Trivial", "Low", "Medium", "High", "Very high"};
    private static final String[] PRIORITY_COLORS = {"#95a5a6", "#27ae60", "#f1c40f", "#e67e22", "#e74c3c"};

    private final ApiClient    apiClient = ApiClient.getInstance();
    private final ObjectMapper mapper    = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<Circle> priorityCircles = new ArrayList<>();

    private int      currentProjectId;
    private int      selectedPriority = -1;
    private Runnable onTaskCreated;

    @FXML
    public void initialize() {
        buildPriorityButtons();
        configureAssigneeCombo();
        titleField.textProperty().addListener((o, a, b) -> validateForm());
        descriptionField.textProperty().addListener((o, a, b) -> validateForm());
        deadlinePicker.valueProperty().addListener((o, a, b) -> validateForm());
        playOpenAnimation();

        overlayPane.widthProperty().addListener((obs, oldV, newV) -> {
            popupCard.setMaxWidth(Math.min(680, newV.doubleValue() * 0.8));
        });

        overlayPane.heightProperty().addListener((obs, oldV, newV) -> {
            popupCard.setMaxHeight(Math.min(800, newV.doubleValue() * 0.8));
        });
    }

    public void setup(int projectId, Runnable onTaskCreated) {
        this.currentProjectId = projectId;
        this.onTaskCreated    = onTaskCreated;
        loadProjectUsers();
    }

    private void buildPriorityButtons() {
        for (int i = 0; i < PRIORITY_LABELS.length; i++) {
            final int value = i;
            final String color = PRIORITY_COLORS[i];

            Circle circle = new Circle(18);
            circle.setFill(Color.web("#1a3a56"));
            circle.setStroke(Color.web(color));
            circle.setStrokeWidth(2.5);
            priorityCircles.add(circle);

            Button btn = new Button();
            btn.setGraphic(circle);
            btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;");
            btn.setOnAction(e -> selectPriority(value, color));

            Label lbl = new Label(PRIORITY_LABELS[i]);
            lbl.setStyle("-fx-text-fill: #8ab0c2; -fx-font-size: 10px;");

            VBox wrapper = new VBox(5, btn, lbl);
            wrapper.setAlignment(Pos.CENTER);
            priorityBox.getChildren().add(wrapper);
        }
    }

    private void selectPriority(int value, String color) {
        priorityCircles.forEach(c -> c.setFill(Color.web("#1a3a56")));
        priorityCircles.get(value).setFill(Color.web(color));
        selectedPriority = value;
        validateForm();
    }

    private void configureAssigneeCombo() {
        assigneeCombo.setConverter(new StringConverter<>() {
            @Override public String toString(UserDto u) {
                return u == null ? "" : u.getNickname() + " (" + u.getName() + ")";
            }
            @Override public UserDto fromString(String s) { return null; }
        });
        assigneeCombo.valueProperty().addListener((o, a, b) -> validateForm());
    }

    private void loadProjectUsers() {
        new Thread(() -> {
            try {
                var resp = apiClient.get("/projects/" + currentProjectId + "/users");
                if (apiClient.isSuccess(resp)) {
                    List<UserDto> users = mapper.readValue(resp.body(), new TypeReference<>() {});
                    Platform.runLater(() -> assigneeCombo.getItems().setAll(users));
                }
            } catch (Exception e) {}
        }).start();
    }

    private void validateForm() {
        boolean valid = !titleField.getText().isBlank()
                && !descriptionField.getText().isBlank()
                && deadlinePicker.getValue() != null
                && selectedPriority != -1
                && assigneeCombo.getValue() != null;

        createBtn.setDisable(!valid);
        createBtn.setStyle(
            "-fx-background-color:" + (valid ? "#6aa896" : "#3a5a4a") + ";" +
            "-fx-text-fill:white;-fx-font-size:15px;-fx-font-weight:bold;" +
            "-fx-background-radius:22;-fx-padding:12 32;" +
            "-fx-cursor:" + (valid ? "hand" : "default") + ";"
        );
    }

    @FXML
    private void handleAiSuggest() {
        String skills = skillsField.getText().trim();
        if (skills.isBlank()) { showAiError(com.rabbit.client.Config.getInstance().getBundle().getString("enter_skill")); return; }

        setAiLoading(true);
        clearAiResults();

        new Thread(() -> {
            try {
                var usersResp = apiClient.get("/projects/" + currentProjectId + "/users");
                if (!apiClient.isSuccess(usersResp)) { Platform.runLater(() -> setAiLoading(false)); return; }

                List<UserDto> users = mapper.readValue(usersResp.body(), new TypeReference<>() {});
                List<String> requiredSkills = Arrays.stream(skills.split(","))
                        .map(String::trim).filter(s -> !s.isBlank()).toList();

                List<Map<String, Object>> workers = users.stream().map(u -> {
                    Map<String, Object> w = new HashMap<>();
                    w.put("id",   u.getId());
                    w.put("name", u.getNickname() != null ? u.getNickname() : u.getName());
                    w.put("skills", List.of());
                    w.put("active_tasks", 0);
                    w.put("max_tasks", 5);
                    w.put("past_tasks", List.of());
                    return w;
                }).toList();

                Map<String, Object> body = Map.of(
                    "description",     descriptionField.getText().trim(),
                    "required_skills", requiredSkills,
                    "workers",         workers
                );

                var aiResp = apiClient.post("/ai/suggest", mapper.writeValueAsString(body));
                if (apiClient.isSuccess(aiResp)) {
                    Map<String, Object> result = mapper.readValue(aiResp.body(), new TypeReference<>() {});
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
            var entry      = ranked.get(i);
            long id        = ((Number) entry.get("worker_id")).longValue();
            String name    = (String) entry.getOrDefault("worker_name", "Unknown");
            String expl    = (String) entry.getOrDefault("explanation", "");
            UserDto target = userMap.get(id);

            Label rankLbl = new Label((i + 1) + ".");
            rankLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;-fx-min-width:20;");
            Label nameLbl = new Label(name);
            nameLbl.setStyle("-fx-text-fill:white;-fx-font-weight:bold;-fx-font-size:14px;");
            HBox.setHgrow(nameLbl, Priority.ALWAYS);
            Label avatar = new Label("\uD83D\uDC64");
            avatar.setStyle("-fx-font-size:18px;-fx-opacity:0.7;");

            HBox left = new HBox(10, rankLbl, nameLbl, avatar);
            left.setAlignment(Pos.CENTER_LEFT);
            left.setPadding(new Insets(12, 14, 12, 14));
            left.setStyle("-fx-background-color:#0a1f33;-fx-background-radius:9 0 0 9;-fx-min-width:170;-fx-cursor:hand;");

            if (target != null) {
                left.setOnMouseClicked(e -> assigneeCombo.setValue(target));
                left.setOnMouseEntered(e -> left.setStyle("-fx-background-color:#1a3a56;-fx-background-radius:9 0 0 9;-fx-min-width:170;-fx-cursor:hand;"));
                left.setOnMouseExited(e  -> left.setStyle("-fx-background-color:#0a1f33;-fx-background-radius:9 0 0 9;-fx-min-width:170;-fx-cursor:hand;"));
            }

            Label explLbl = new Label("• " + expl);
            explLbl.setStyle("-fx-text-fill:#333;-fx-font-size:13px;");
            explLbl.setWrapText(true);
            explLbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(explLbl, Priority.ALWAYS);

            HBox right = new HBox(explLbl);
            right.setAlignment(Pos.CENTER_LEFT);
            right.setPadding(new Insets(12, 14, 12, 14));
            right.setStyle("-fx-background-color:white;-fx-background-radius:0 9 9 0;");
            HBox.setHgrow(right, Priority.ALWAYS);

            final double COLLAPSED_HEIGHT = 22.0;
            explLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
            explLbl.setMinHeight(COLLAPSED_HEIGHT);
            explLbl.setPrefHeight(COLLAPSED_HEIGHT);
            explLbl.setMaxHeight(COLLAPSED_HEIGHT);

            final boolean[] expanded = {false};
            javafx.event.EventHandler<MouseEvent> toggleHandler = e -> {
                if (!expanded[0]) {
                    Platform.runLater(() -> {
                        explLbl.applyCss();
                        double availableWidth = right.getWidth() - (right.getPadding().getLeft() + right.getPadding().getRight());
                        if (availableWidth <= 0) {
                            availableWidth = popupCard.getWidth() - 200;
                        }
                        explLbl.setPrefWidth(availableWidth);

                        Text measureText = new Text(explLbl.getText());
                        measureText.setFont(explLbl.getFont());
                        measureText.setWrappingWidth(availableWidth);

                        double explanationHeight = Math.ceil(measureText.getLayoutBounds().getHeight());
                        Insets padding = right.getPadding();
                        double targetH = Math.max(
                                explanationHeight + padding.getTop() + padding.getBottom() + 4,
                                COLLAPSED_HEIGHT
                        );
                        explLbl.setMinHeight(targetH);
                        explLbl.setPrefHeight(targetH);
                        explLbl.setMaxHeight(targetH);
                        expanded[0] = true;
                        right.requestLayout();
                        aiResultsBox.requestLayout();
                    });
                } else {
                    explLbl.setTextOverrun(OverrunStyle.ELLIPSIS);
                    explLbl.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    explLbl.setMinHeight(COLLAPSED_HEIGHT);
                    explLbl.setPrefHeight(COLLAPSED_HEIGHT);
                    explLbl.setMaxHeight(COLLAPSED_HEIGHT);
                    expanded[0] = false;
                    Platform.runLater(() -> { right.requestLayout(); aiResultsBox.requestLayout(); });
                }
                e.consume();
            };

            right.setCursor(Cursor.HAND);
            explLbl.setCursor(Cursor.HAND);
            explLbl.setOnMouseClicked(toggleHandler);
            right.setOnMouseClicked(toggleHandler);

            HBox row = new HBox(left, right);
            row.setStyle("-fx-border-color:#3E6273;-fx-border-radius:10;-fx-border-width:2;-fx-background-radius:10;");
            VBox.setMargin(row, new Insets(i == 0 ? 4 : 8, 0, 0, 0));

            aiResultsBox.getChildren().add(row);
        }

        aiResultsTitle.setVisible(true);
        aiResultsTitle.setManaged(true);
        showAiResults();
    }

    private void clearAiResults() {
        if (aiResultsBox.getChildren().size() > 1)
            aiResultsBox.getChildren().remove(1, aiResultsBox.getChildren().size());
        aiResultsBox.setVisible(false);
        aiResultsBox.setManaged(false);
    }


    

    private void showAiResults() {
        aiResultsBox.setVisible(true);
        aiResultsBox.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(300), aiResultsBox);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void showAiError(String msg) {
        clearAiResults();
        aiResultsTitle.setVisible(false);
        aiResultsTitle.setManaged(false);
        Label err = new Label("⚠  " + msg);
        err.setStyle("-fx-text-fill:#e74c3c;-fx-font-size:12px;");
        VBox.setMargin(err, new Insets(4, 0, 0, 0));
        aiResultsBox.getChildren().add(err);
        showAiResults();
    }

    private void setAiLoading(boolean loading) {
        aiLoadingIndicator.setVisible(loading);
        aiLoadingIndicator.setManaged(loading);
        aiSuggestBtn.setDisable(loading);
    }

    @FXML
    private void handleCreateTask() {
        UserDto assignee = assigneeCombo.getValue();
        if (assignee == null || selectedPriority == -1) return;

        String deadline = LocalDateTime.of(deadlinePicker.getValue(), LocalTime.of(23, 59))
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title",       titleField.getText().trim());
        payload.put("description", descriptionField.getText().trim());
        payload.put("deadline",     deadline);
        payload.put("priority",    selectedPriority);
        payload.put("assignedTo",  assignee.getId());
        payload.put("status",      "backlog");

        createBtn.setDisable(true);
        createBtn.setText(com.rabbit.client.Config.getInstance().getBundle().getString("creating"));

        new Thread(() -> {
            try {
                var resp = apiClient.post("/tasks/" + currentProjectId + "/create",
                        mapper.writeValueAsString(payload));
                Platform.runLater(() -> {
                    if (apiClient.isSuccess(resp)) {
                        if (onTaskCreated != null) onTaskCreated.run();
                        closePopup();
                    } else {
                        createBtn.setDisable(false);
                        createBtn.setText(com.rabbit.client.Config.getInstance().getBundle().getString("create_task_btn_text"));
                        MainController mc = MainController.getInstance();
                        if (mc != null) mc.showGlobalNotification(com.rabbit.client.Config.getInstance().getBundle().getString("update_failed") + ": " + resp.statusCode(), "#ED4245");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    createBtn.setText(com.rabbit.client.Config.getInstance().getBundle().getString("create_task_btn_text"));
                });
            }
        }).start();
    }

    @FXML private void handleCancel() { closePopup(); }
    @FXML private void handleOverlayClick(MouseEvent e) { if (e.getTarget() == overlayPane) closePopup(); }
    @FXML private void consumeClick(MouseEvent e) { e.consume(); }

    private void closePopup() {
        FadeTransition  fade  = new FadeTransition(Duration.millis(160), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(160), popupCard);
        fade.setToValue(0); scale.setToX(0.85); scale.setToY(0.85);
        fade.setOnFinished(e -> { if (overlayPane.getParent() instanceof Pane p) p.getChildren().remove(overlayPane); });
        fade.play(); scale.play();
    }

    private void playOpenAnimation() {
        overlayPane.setOpacity(0); popupCard.setScaleX(0.85); popupCard.setScaleY(0.85);
        FadeTransition  fade  = new FadeTransition(Duration.millis(200), overlayPane);
        ScaleTransition scale = new ScaleTransition(Duration.millis(220), popupCard);
        fade.setToValue(1); scale.setToX(1); scale.setToY(1);
        fade.play(); scale.play();
    }
}