package com.rabbit.client.ui.controllers;

import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.enums.TaskStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class MyTasksController {

    @FXML
    private TableView<TaskDto> tasksTable;
    @FXML
    private TableColumn<TaskDto, String> typeColumn;
    @FXML
    private TableColumn<TaskDto, String> titleColumn;
    @FXML
    private TableColumn<TaskDto, String> descriptionColumn;
    @FXML
    private TableColumn<TaskDto, String> statusColumn;
    @FXML
    private TableColumn<TaskDto, String> projectColumn;
    @FXML
    private TableColumn<TaskDto, String> deadlineColumn;
    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        setupColumns();
        ObservableList<TaskDto> testTasks = FXCollections.observableArrayList(
                new TaskDto(1, 101, 2, 1, "smth", "smth", 3, TaskStatus.IN_PROGRESS.getValue(), Timestamp.from(Instant.now().plus(2, ChronoUnit.DAYS))),
                new TaskDto(4, 103, 2, 4, "smth", "smth", 1, TaskStatus.ASSIGNED.getValue(), Timestamp.from(Instant.now().plus(10, ChronoUnit.DAYS)))
        );
        tasksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tasksTable.setItems(testTasks);
        statusLabel.setText("Loaded " + testTasks.size() + " tasks.");

    }
    private void setupColumns() {
        typeColumn.setCellValueFactory(cellData -> {
            int priority = cellData.getValue().getPriority();
            String type = priority > 2 ? "Bug" : "Feature";
            return new SimpleStringProperty(type);
        });

        titleColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTitle())
        );

        descriptionColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDescription())
        );

        statusColumn.setCellValueFactory(cellData -> {
            String rawStatus = cellData.getValue().getStatus();
            String displayStatus = rawStatus.replace("_", " ");
            return new SimpleStringProperty(displayStatus);
        });

        projectColumn.setCellValueFactory(cellData -> {
            int projectId = cellData.getValue().getProjectId();
            return new SimpleStringProperty("VAVA Project #" + projectId);
        });

        deadlineColumn.setCellValueFactory(cellData -> {
            Timestamp deadline = cellData.getValue().getDeadline();
            if (deadline != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
                return new SimpleStringProperty(sdf.format(deadline));
            }
            return new SimpleStringProperty("No deadline");
        });
    }

}