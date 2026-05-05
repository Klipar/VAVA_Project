package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerResultDto {
    private int worker_id;
    private String worker_name;
    private float final_score;
    private String explanation;
    private ScoreBreakdownDto breakdown;
}
