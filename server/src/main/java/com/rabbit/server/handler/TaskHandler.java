package com.rabbit.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbit.common.dto.TaskDto;
import com.rabbit.common.dto.TaskRequestDto;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.TaskService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskHandler {

    private final TaskService service = new TaskService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();

    // GET /tasks/{projectId}
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
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                List<TaskDto> tasks = service.getTasksByProjectId(projectId, userId);
                send(exchange, 200, mapper.writeValueAsString(tasks));
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // POST /tasks/{projectId}/create
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
                int projectId = extractId(exchange.getRequestURI().getPath(), 2);
                TaskRequestDto requestDto = mapper.readValue(exchange.getRequestBody(), TaskRequestDto.class);
                TaskDto createdTask = service.createTask(projectId, userId, requestDto);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "Task created successfully");
                response.put("task", createdTask);

                send(exchange, 201, mapper.writeValueAsString(response));
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // PUT /tasks/{taskId}/update
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
                int taskId = extractId(exchange.getRequestURI().getPath(), 2);
                TaskRequestDto requestDto = mapper.readValue(exchange.getRequestBody(), TaskRequestDto.class);
                TaskDto updatedTask = service.updateTask(taskId, userId, requestDto);

                if (updatedTask != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Task updated successfully");
                    response.put("task", updatedTask);
                    send(exchange, 200, mapper.writeValueAsString(response));
                } else {
                    send(exchange, 404, "{\"error\":\"Task not found\"}");
                }
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // PATCH or PUT /tasks/{taskId}/status
    public HttpHandler updateStatus() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("PUT") && !exchange.getRequestMethod().equals("PATCH")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer userId = resolveUserId(exchange);
            if (userId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                int taskId = extractId(exchange.getRequestURI().getPath(), 2);

                Map<String, String> body = mapper.readValue(exchange.getRequestBody(), new com.fasterxml.jackson.core.type.TypeReference<>() {});
                String newStatus = body.get("status");

                if (newStatus == null) {
                    send(exchange, 400, "{\"error\":\"Status field is required\"}");
                    return;
                }

                TaskDto updatedTask = service.updateTaskStatus(taskId, userId, newStatus);

                if (updatedTask != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Status updated successfully");
                    response.put("task", updatedTask);
                    send(exchange, 200, mapper.writeValueAsString(response));
                } else {
                    send(exchange, 404, "{\"error\":\"Task not found\"}");
                }
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // DELETE /tasks/{taskId}/delete
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
                int taskId = extractId(exchange.getRequestURI().getPath(), 2);
                TaskDto deletedTask = service.deleteTask(taskId, userId);

                if (deletedTask != null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Task deleted successfully");
                    response.put("task", deletedTask);
                    send(exchange, 200, mapper.writeValueAsString(response));
                } else {
                    send(exchange, 404, "{\"error\":\"Task not found\"}");
                }
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
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