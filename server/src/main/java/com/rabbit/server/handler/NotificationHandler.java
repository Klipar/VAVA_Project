package com.rabbit.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbit.common.dto.NotificationDto;
import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.NotificationService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

public class NotificationHandler {

    private final NotificationService service = new NotificationService();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();

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
                List<NotificationDto> notifications = service.getNotificationsForUser(userId);
                send(exchange, 200, mapper.writeValueAsString(notifications));
            } catch (SQLException e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Database error\"}");
            } catch (Exception e) {
                e.printStackTrace();
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }
        };
    }


    public HttpHandler markRead() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("PUT")) { send(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }

            Integer userId = resolveUserId(exchange);
            if (userId == null) { send(exchange, 401, "{\"error\":\"Unauthorized\"}"); return; }

            try {
                long notificationId = extractId(exchange.getRequestURI().getPath(), 2);
                boolean updated = service.markAsRead(userId, notificationId);
                send(exchange, updated ? 200 : 404,
                        updated ? "{\"message\":\"Marked as read\"}" : "{\"error\":\"Notification not found or does not belong to you\"}");
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

    private long extractId(String path, int segment) {
        return Long.parseLong(path.split("/")[segment]);
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
