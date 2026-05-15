package com.rabbit.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;

import com.rabbit.common.dto.AiRequestDto;
import com.rabbit.common.dto.AiResponseDto;
import com.rabbit.common.dto.PastTaskDto;

public class AiProxyService {
    private static final String AI_URL = "http://localhost:8000/suggest";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiResponseDto suggest(AiRequestDto request) throws IOException, InterruptedException {
        request = loadPastExperience(request);

        String requestBody = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(AI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), AiResponseDto.class);
    }

    private AiRequestDto loadPastExperience(AiRequestDto dto){
        DatabaseService databaseService = DatabaseService.getInstance();
        dto.getWorkers().replaceAll(
            (worker) -> {
                try {
                    worker.setPast_tasks(
                        databaseService.query("""
                        SELECT
                        t.description
                        FROM
                        tasks AS t
                        WHERE t.assigned_to = ? AND t.status = 'done'
                        """, worker.getId())
                        .stream()
                        .map(row -> new PastTaskDto(
                            (String) row.get("description")
                        ))
                        .toList()
                    );
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return worker;
            }
        );

        return dto;
    }
}
