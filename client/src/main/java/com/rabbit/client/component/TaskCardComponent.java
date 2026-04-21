package com.rabbit.client.component;

import com.rabbit.common.dto.TaskDto;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.Objects;

public class TaskCardComponent extends VBox {
    private final TaskDto task;

    public TaskCardComponent(TaskDto task) {
        this.task = task;
        this.getStyleClass().add("task-card");
        this.setSpacing(10);

        this.setAccessibleText(String.valueOf(task.getId()));

        Label title = new Label(task.getTitle().toUpperCase());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        ImageView calIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/calendar.png"))));
        calIcon.setFitWidth(16);
        calIcon.setFitHeight(16);
        Label dateLabel = new Label(task.getDeadline());
        dateLabel.setStyle("-fx-text-fill: #99AAB5; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(calIcon, dateLabel);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);
        Label priorityLabel = new Label("Priority: ");
        priorityLabel.setStyle("-fx-text-fill: #99AAB5; -fx-font-size: 12px;");
        Label priorityVal = new Label(getPriorityText(task.getPriority()));
        priorityVal.setStyle("-fx-text-fill: #FF9F0A; -fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ImageView userIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/user.png"))));
        userIcon.setFitWidth(20);
        userIcon.setFitHeight(20);

        footer.getChildren().addAll(priorityLabel, priorityVal, spacer, userIcon);
        this.getChildren().addAll(title, dateBox, footer);
    }

    public TaskDto getTask() { return task; }

    private String getPriorityText(int p) {
        if (p >= 3) return "HIGH";
        if (p == 2) return "MEDIUM";
        return "LOW";
    }
}