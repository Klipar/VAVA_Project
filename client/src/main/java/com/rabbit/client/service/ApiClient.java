package com.rabbit.client.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.client.Config;
import com.rabbit.common.dto.NotificationDto;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class ApiClient {
    private static ApiClient instance;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Config config;
    private UserService userService;

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.config = Config.getInstance();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    private UserService getUserService() {
        if (userService == null) {
            userService = UserService.getInstance();
        }
        return userService;
    }

    public HttpResponse<String> postPublic(String url, String body) throws Exception {
        String fullUrl = config.getBaseUrl() + url;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body != null ? body : ""))
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getPublic(String url) throws Exception {
        String fullUrl = config.getBaseUrl() + url;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getAuthenticated(String url, String token) throws Exception {
        String fullUrl = config.getBaseUrl() + url;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendAuthenticatedRequest(String url, String method, String body) throws Exception {
        String token = getUserService().getToken();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("User not authenticated");
        }

        String fullUrl = config.getBaseUrl() + url;
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30));

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

    public boolean isSuccess(HttpResponse<String> response) {
        int code = response.statusCode();
        return code >= 200 && code < 300;
    }

    public boolean isUnauthorized(HttpResponse<String> response) {
        return response.statusCode() == 401;
    }

    public boolean isForbidden(HttpResponse<String> response) {
        return response.statusCode() == 403;
    }

    public boolean isNotFound(HttpResponse<String> response) {
        return response.statusCode() == 404;
    }

    public List<NotificationDto> getNotifications() throws Exception {
        HttpResponse<String> response = get("/notifications");
        if (isSuccess(response)) {
            return objectMapper.readValue(response.body(), new TypeReference<List<NotificationDto>>() {});
        }
        return List.of();
    }

    public boolean markNotificationAsRead(long notificationId) throws Exception {
        HttpResponse<String> response = put("/notifications/" + notificationId + "/read", "{}");
        return isSuccess(response);
    }
}