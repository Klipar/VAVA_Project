package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestDto {
    private int assignedTo;
    private String title;
    private String description;
    private int priority;
    private String status;
    private String deadline;
}