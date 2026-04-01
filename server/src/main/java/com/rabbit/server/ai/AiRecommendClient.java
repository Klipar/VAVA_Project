package com.rabbit.server.ai;

import java.net.http.HttpClient;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AiRecommendClient {
    private static final String SERVER_URL = "http://localhost:6969/ai/suggest";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public String getSuggestion(String requestBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(SERVER_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}
