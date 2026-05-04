package com.rabbit.server.handler;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.dto.ProjectDto;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.ProjectService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ProjectHandler {

    private final ProjectService service = new ProjectService();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();


    public HttpHandler getUserRole() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                String role = service.getUserRoleInProject(projectId, userId);

                Map<String, String> response = new HashMap<>();
                response.put("role", role);
                send(exchange, 200, mapper.writeValueAsString(response));
            } catch (IllegalArgumentException e) {
                send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    public HttpHandler getAll() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            try {
                String[] segments = exchange.getRequestURI().getPath().split("/");
                if (segments.length >= 3 && !segments[2].isEmpty()) {
                    int projectId = Integer.parseInt(segments[2]);
                    Optional<ProjectDto> project = service.getProjectById(projectId, userId);
                    if (project.isEmpty()) {
                        send(exchange, 404, "{\"error\":\"Project not found\"}");
                        return;
                    }
                    send(exchange, 200, mapper.writeValueAsString(project.get()));
                } else {
                    List<ProjectDto> projects = service.getProjectsForUser(userId);
                    send(exchange, 200, mapper.writeValueAsString(projects));
                }
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (NumberFormatException e) {
                send(exchange, 400, "{\"error\":\"Invalid project ID\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    public HttpHandler create() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            try {
                ProjectDto dto = mapper.readValue(exchange.getRequestBody(), ProjectDto.class);
                dto.setId(0);
                ProjectDto created = service.createProject(userId, dto);
                send(exchange, 201, mapper.writeValueAsString(created));
            } catch (IllegalArgumentException e) {
                send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }


    public HttpHandler update() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("PUT")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                ProjectDto dto = mapper.readValue(exchange.getRequestBody(), ProjectDto.class);
                ProjectDto updated = service.updateProject(projectId, userId, dto);
                send(exchange, 200, mapper.writeValueAsString(updated));
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }
    public HttpHandler delete() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }
            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            try {
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                service.deleteProject(projectId, userId);
                send(exchange, 200, "{\"message\":\"Project deleted\"}");
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    private Integer resolveUserId(HttpExchange exchange) {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        return auth.getUserId(authHeader.substring(7));
    }

    private int extractId(String path, int segment) {
        return Integer.parseInt(path.split("/")[segment]);
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
