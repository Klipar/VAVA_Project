package com.rabbit.client.component;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.enums.Priority; // Імпортуємо наш енум
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class TaskCardComponent extends VBox {
    @Getter
    private final TaskDto task;
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy, HH:mm", Locale.ENGLISH);

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
        calIcon.setFitWidth(16); calIcon.setFitHeight(16);

        String formattedDate = formatDeadline(task.getDeadline());
        Label dateLabel = new Label(formattedDate);
        dateLabel.setStyle("-fx-text-fill: #99AAB5; -fx-font-size: 12px;");
        dateBox.getChildren().addAll(calIcon, dateLabel);

        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER_LEFT);

        Label priorityLabel = new Label("Priority: ");
        priorityLabel.setStyle("-fx-text-fill: #99AAB5; -fx-font-size: 12px;");

        Priority p = Priority.fromLevel(task.getPriority());

        Label priorityVal = new Label(p.name());

        priorityVal.setStyle("-fx-text-fill: " + p.getColor() + "; -fx-font-weight: bold; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        ImageView userIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/com/rabbit/client/images/user.png"))));
        userIcon.setFitWidth(20); userIcon.setFitHeight(20);

        footer.getChildren().addAll(priorityLabel, priorityVal, spacer, userIcon);

        this.getChildren().addAll(title, dateBox, footer);
    }

    private String formatDeadline(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isEmpty() || "null".equals(deadlineStr)) return "No deadline";
        try {
            if (!deadlineStr.endsWith("Z") && !deadlineStr.contains("+")) {
                deadlineStr += "Z";
            }
            ZonedDateTime zdt = ZonedDateTime.parse(deadlineStr);
            return zdt.format(displayFormatter);
        } catch (Exception e) {
            return deadlineStr;
        }
    }
}