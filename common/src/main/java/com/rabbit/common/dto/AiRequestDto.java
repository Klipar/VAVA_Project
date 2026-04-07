package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiRequestDto {
    private String description;
    private List<String> required_skills;
    private List<WorkerDto> workers;
}