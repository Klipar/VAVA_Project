package com.rabbit.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.dto.UserDto;
import com.rabbit.common.enums.UserRole;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.UserService;
import com.rabbit.server.repository.UserRepository;
import com.rabbit.server.service.DatabaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class UserHandler {

    private final UserService service;
    private final ObjectMapper mapper;
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();

    public UserHandler(UserRepository userRepository) {
        this.service = new UserService(userRepository);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // GET /users/{userId}
    public HttpHandler getUser() {
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
                long targetUserId = extractId(exchange.getRequestURI().getPath(), 2);
                UserDto user = service.getUser(targetUserId);
                send(exchange, 200, mapper.writeValueAsString(user));
            } catch (IllegalArgumentException e) {
                send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // GET /projects/{projectId}/users
    public HttpHandler getAllUsersFromProject() {
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
                long projectId = extractId(exchange.getRequestURI().getPath(), 2);
                List<UserDto> users = service.getAllUsersFromProject(projectId);
                send(exchange, 200, mapper.writeValueAsString(users));
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // POST /projects/{projectId}/users/create
    public HttpHandler createUser() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer creatorId = resolveUserId(exchange);
            if (creatorId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                long projectId = extractId(exchange.getRequestURI().getPath(), 2);

                // Отримуємо роль творця з бази даних
                UserRole creatorRole = getUserRole(creatorId.longValue());

                // Парсимо тіло запиту
                Map<String, Object> requestBody = mapper.readValue(exchange.getRequestBody(), Map.class);

                UserDto userDto = new UserDto();
                userDto.setName((String) requestBody.get("name"));
                userDto.setNickname((String) requestBody.get("nickname"));
                userDto.setEmail((String) requestBody.get("email"));

                String password = (String) requestBody.get("password");

                UserDto createdUser = service.createUser(userDto, password, creatorRole, projectId);
                service.addUserToProject(createdUser.getId(), projectId);

                send(exchange, 201, mapper.writeValueAsString(createdUser));
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // PUT /users/{userId}/update
    public HttpHandler updateUser() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("PUT")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer requestingUserId = resolveUserId(exchange);
            if (requestingUserId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                long targetUserId = extractId(exchange.getRequestURI().getPath(), 2);
                UserDto updatedData = mapper.readValue(exchange.getRequestBody(), UserDto.class);

                UserDto updatedUser = service.updateUser(targetUserId, updatedData, requestingUserId.longValue());
                send(exchange, 200, mapper.writeValueAsString(updatedUser));
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // DELETE /users/{userId}/delete
    public HttpHandler deleteUser() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer requestingUserId = resolveUserId(exchange);
            if (requestingUserId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                long targetUserId = extractId(exchange.getRequestURI().getPath(), 2);

                // Видаляємо зв'язки з проектами перед видаленням юзера
                service.removeUserFromAllProjects(targetUserId);

                // Видаляємо юзера
                service.deleteUser(targetUserId, requestingUserId.longValue());

                send(exchange, 200, "{\"message\":\"User deleted successfully\"}");
            } catch (SecurityException e) {
                send(exchange, 403, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 404, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // POST /projects/{projectId}/users/{userId}/add
    public HttpHandler addUserToProject() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer requestingUserId = resolveUserId(exchange);
            if (requestingUserId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                long projectId = extractId(path, 2);
                long userId = extractId(path, 4);

                // Перевірка чи користувач має права додавати юзера до проекту
                UserRole requesterRole = getUserRole(requestingUserId.longValue());
                if (requesterRole != UserRole.MANAGER && requesterRole != UserRole.TEAM_LEADER) {
                    send(exchange, 403, "{\"error\":\"Only Manager or Team Leader can add users to project\"}");
                    return;
                }

                service.addUserToProject(userId, projectId);
                send(exchange, 200, "{\"message\":\"User added to project successfully\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
            } catch (Exception e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }

    // DELETE /projects/{projectId}/users/{userId}/remove
    public HttpHandler removeUserFromProject() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("DELETE")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            Integer requestingUserId = resolveUserId(exchange);
            if (requestingUserId == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                String path = exchange.getRequestURI().getPath();
                long projectId = extractId(path, 2);
                long userId = extractId(path, 4);

                // Тільки сам користувач може видалити себе з проекту
                if (userId != requestingUserId.longValue()) {
                    send(exchange, 403, "{\"error\":\"You can only remove yourself from project\"}");
                    return;
                }

                service.removeUserFromProject(userId, projectId);
                send(exchange, 200, "{\"message\":\"User removed from project successfully\"}");
            } catch (IllegalArgumentException e) {
                send(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
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

    private long extractId(String path, int segment) {
        String[] parts = path.split("/");
        if (segment >= parts.length) {
            throw new IllegalArgumentException("Invalid path");
        }
        return Long.parseLong(parts[segment]);
    }

    private UserRole getUserRole(Long userId) {
        // Отримуємо роль користувача з бази даних
        return service.getUser(userId).getRole();
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