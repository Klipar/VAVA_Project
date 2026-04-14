package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerDto {
    private int id;
    private String name;
    private List<String> skills;
    private int active_tasks;
    private int max_tasks = 5; // Default max tasks per worker
    private List<PastTaskDto> past_tasks; // List of past task descriptions for better AI recommendations
}
