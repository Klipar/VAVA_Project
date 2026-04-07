package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreBreakdownDto {
    private float text_similarity;
    private String most_similar_task;
    private float skill_overlap;
    private List<String> matched_skills;
    private List<String> missing_skills;
    private String match_ratio;
    private float workload_score;
    private int active_tasks;
}
