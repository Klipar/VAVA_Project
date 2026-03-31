package com.rabbit.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private int id;
    private String title;
    private String description;
    private Timestamp deadline;
    private String status;
}