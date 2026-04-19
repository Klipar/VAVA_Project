package com.rabbit.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.model.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
    private static ApiClient instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    private ApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.userService = UserService.getInstance();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    public HttpResponse<String> sendAuthenticatedRequest(String url, String method, String body) throws Exception {
        UserSession session = userService.getCurrentSession();
        if (session == null || !session.isValid()) {
            throw new IllegalStateException("User not authenticated");
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + session.getToken())
                .header("Content-Type", "application/json");

        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.GET();
                break;
            case "POST":
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "PUT":
                requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body != null ? body : ""));
                break;
            case "DELETE":
                requestBuilder.DELETE();
                break;
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }

        HttpRequest request = requestBuilder.build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> get(String url) throws Exception {
        return sendAuthenticatedRequest(url, "GET", null);
    }

    public HttpResponse<String> post(String url, String body) throws Exception {
        return sendAuthenticatedRequest(url, "POST", body);
    }

    public HttpResponse<String> put(String url, String body) throws Exception {
        return sendAuthenticatedRequest(url, "PUT", body);
    }

    public HttpResponse<String> delete(String url) throws Exception {
        return sendAuthenticatedRequest(url, "DELETE", null);
    }
}