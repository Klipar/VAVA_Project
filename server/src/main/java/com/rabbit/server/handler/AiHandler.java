package com.rabbit.server.handler;

import com.rabbit.server.middleware.AuthMiddleware;
import com.rabbit.server.service.AiProxyService;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import com.rabbit.common.dto.AiRequestDto;
import com.rabbit.common.dto.AiResponseDto;

public class AiHandler {
    private final AiProxyService service = new AiProxyService();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthMiddleware auth = AuthMiddleware.getInstanse();

    // POST /ai/suggest
    public HttpHandler suggest() {
        return exchange -> {
            if (!exchange.getRequestMethod().equals("POST")) {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ") || auth.getUserId(authHeader.substring(7)) == null) {
                send(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                AiRequestDto request = objectMapper.readValue(requestBody, AiRequestDto.class);
                AiResponseDto aiResponse = service.suggest(request);
                send(exchange, 200, objectMapper.writeValueAsString(aiResponse));
            } catch (IOException | InterruptedException e) {
                send(exchange, 500, "{\"error\":\"Internal server error\"}");
            }

        };
    }

    private void send(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (var os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}