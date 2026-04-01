package com.rabbit.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.rabbit.common.dto.AiRequestDto;
import com.rabbit.common.dto.AiResponseDto;

public class AiProxyService {
    private static final String AI_URL = "http://localhost:8000/suggest";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiResponseDto suggest(AiRequestDto request) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(request);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(java.net.URI.create(AI_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), AiResponseDto.class);
    }
}
