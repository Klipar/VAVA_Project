package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private int id;
    private int projectId;
    private int assignedTo;
    private int createdBy;
    private String title;
    private String description;
    private int priority;
    private String status;
    private Timestamp deadline;
}
