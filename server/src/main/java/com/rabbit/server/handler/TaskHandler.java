package com.rabbit.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class TaskHandler {

    private final TaskService service = new TaskService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();

    // GET /tasks/{projectId}
    public HttpHandler getAll() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("GET")) { send(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

            Integer userId = resolveUserId(exchange);
            if (userId == null) { send(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

            try {
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                List<TaskDto> tasks = service.getTasksByProjectId(projectId);
                send(exchange, 200, mapper.writeValueAsString(tasks));
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // POST /tasks/{projectId}/create
    public HttpHandler create() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) { send(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

            Integer userId = resolveUserId(exchange);
            if (userId == null) { send(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

            try {
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                TaskDto dto = mapper.readValue(exchange.getRequestBody(), TaskDto.class);
                service.createTask(projectId, userId, dto);
                send(exchange, 201, "{\"message\":\"Task created\"}");
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // PUT /tasks/{taskId}/update
    public HttpHandler update() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("PUT")) { send(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

            Integer userId = resolveUserId(exchange);
            if (userId == null) { send(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

            try {
                int taskId = extractId(exchange.getRequestURI().getPath(), 2);
                TaskDto dto = mapper.readValue(exchange.getRequestBody(), TaskDto.class);
                boolean updated = service.updateTask(taskId, userId, dto);
                send(exchange, updated ? 200 : 404,
                        updated ? "{\"message\":\"Updated\"}" : "{\"error\":\"Task not found\"}");
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // DELETE /tasks/{taskId}/delete
    public HttpHandler delete() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) { send(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

            Integer userId = resolveUserId(exchange);
            if (userId == null) { send(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

            try {
                int taskId = extractId(exchange.getRequestURI().getPath(), 2);
                boolean deleted = service.deleteTask(taskId, userId);
                send(exchange, deleted ? 200 : 404,
                        deleted ? "{\"message\":\"Deleted\"}" : "{\"error\":\"Task not found\"}");
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
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