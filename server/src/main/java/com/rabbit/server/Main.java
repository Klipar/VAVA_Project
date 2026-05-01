package com.rabbit.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.rabbit.server.handler.AiHandler;
import com.rabbit.server.handler.NotificationHandler;
import com.rabbit.server.handler.ProjectHandler;
import com.rabbit.server.handler.TaskHandler;
import com.rabbit.server.handler.UserHandler;
import com.rabbit.server.repository.UserRepository;
import com.rabbit.server.service.DatabaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class Main {
    public static void main(String[] args) throws IOException {
        // Create HTTP server on port 6969
        HttpServer server = HttpServer.create(new InetSocketAddress(6969), 0);

        // Initialize DatabaseService and UserRepository
        DatabaseService databaseService = DatabaseService.getInstance();
        UserRepository userRepository = new UserRepository(databaseService);

        // Initialize handlers
        TaskHandler taskHandler = new TaskHandler();
        AiHandler aiHandler = new AiHandler();
        UserHandler userHandler = new UserHandler(userRepository);
        ProjectHandler projectHandler = new ProjectHandler();
        NotificationHandler notificationHandler = new NotificationHandler();

        // ============ TASK ENDPOINTS ============
        server.createContext("/tasks/", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();
            try {
                if (path.matches("/tasks/\\d+$") && method.equals("GET")) {
                    taskHandler.getAll().handle(exchange);
                }else if (path.matches("/tasks/\\d+/create$") && method.equals("POST")) {
                    taskHandler.create().handle(exchange);
                }
                else if (path.matches("/tasks/\\d+/status$") && (method.equals("PUT") || method.equals("PATCH"))) {
                    taskHandler.updateStatus().handle(exchange);
                }
                else if (path.matches("/tasks/\\d+/update$") && method.equals("PUT")) {
                    taskHandler.update().handle(exchange);
                } else if (path.matches("/tasks/\\d+/delete$") && method.equals("DELETE")) {
                    taskHandler.delete().handle(exchange);
                } else {
                    send405(exchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange);
            }
        });

        // ============ AI ENDPOINT ============
        server.createContext("/ai/suggest", aiHandler.suggest());     // POST /ai/suggest

        // ============ USER ENDPOINTS ============
        // We use a single shared context for all user endpoints
        server.createContext("/users", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // GET /users/nickname/{nickname}
                if (path.matches("/users/nickname/[^/]+$") && method.equals("GET")) {
                    userHandler.getUserByNickname().handle(exchange);
                    return;
                }
                // GET /users/{userId}
                if (path.matches("/users/\\d+$") && method.equals("GET")) {
                    userHandler.getUser().handle(exchange);
                    return;
                }
                // PUT /users/{userId}/update
                else if (path.matches("/users/\\d+/update$") && method.equals("PUT")) {
                    userHandler.updateUser().handle(exchange);
                    return; 
                }
                // POST /users/create
                else if (path.matches("/users/create$") && method.equals("POST")) {
                    userHandler.createUser().handle(exchange);
                }
                // DELETE /users/{userId}/delete
                else if (path.matches("/users/\\d+/delete$") && method.equals("DELETE")) {
                    userHandler.deleteUser().handle(exchange);
                    return;
                }
                else if (path.matches("/users/login") && method.equals("POST")) {
                    userHandler.loginUser().handle(exchange);
                    return;
                }
                else {
                    send405(exchange);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange);
            }
        });

        // ============ PROJECT ENDPOINTS ============
        server.createContext("/projects", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // GET /projects or GET /projects/{id}
                if (path.matches("/projects(/\\d+)?$") && method.equals("GET")) {
                    projectHandler.getAll().handle(exchange);
                }
                // POST /projects
                else if (path.equals("/projects") && method.equals("POST")) {
                    projectHandler.create().handle(exchange);
                }
                // PUT /projects/{id}
                else if (path.matches("/projects/\\d+$") && method.equals("PUT")) {
                    projectHandler.update().handle(exchange);
                }
                // DELETE /projects/{id}
                else if (path.matches("/projects/\\d+$") && method.equals("DELETE")) {
                    projectHandler.delete().handle(exchange);
                }
                // GET /projects/{projectId}/users
                else if (path.matches("/projects/\\d+/users$") && method.equals("GET")) {
                    userHandler.getAllUsersFromProject().handle(exchange);
                }
                // POST /projects/{projectId}/users/{userId}/add
                else if (path.matches("/projects/\\d+/users/\\d+/add$") && method.equals("POST")) {
                    userHandler.addUserToProject().handle(exchange);
                }
                // DELETE /projects/{projectId}/users/{userId}/remove
                else if (path.matches("/projects/\\d+/users/\\d+/remove$") && method.equals("DELETE")) {
                    userHandler.removeUserFromProject().handle(exchange);
                }
                else {
                    send405(exchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange);
            }
        });

        // ============ NOTIFICATION ENDPOINTS ============
        server.createContext("/notifications", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // GET /notifications
                if (path.equals("/notifications") && method.equals("GET")) {
                    notificationHandler.getAll().handle(exchange);
                }
                // PUT /notifications/{id}/read
                else if (path.matches("/notifications/\\d+/read$") && method.equals("PUT")) {
                    notificationHandler.markRead().handle(exchange);
                }
                else {
                    send405(exchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange);
            }
        });

        // ============ DOCUMENTATION ============
        server.createContext("/swagger", new SwaggerHandler());
        server.createContext("/openapi.json", new OpenApiHandler());

        // ============ HELLO ENDPOINT ============
        server.createContext("/hello", new HelloHandler());

        // Start server
        server.setExecutor(null);
        server.start();

        System.out.println("Server started on port 6969");
        System.out.println("API Documentation available at: http://localhost:6969/swagger");
    }

    private static void send405(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Method not allowed\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(405, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void send500(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Internal server error\"}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(500, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String response = "Hello from server";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class SwaggerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>API Documentation</title>
                <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
            </head>
            <body>
                <div id="swagger-ui"></div>
                <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
                <script>
                    window.onload = () => {
                        window.ui = SwaggerUIBundle({
                            url: "/openapi.json",
                            dom_id: "#swagger-ui",
                        });
                    };
                </script>
            </body>
            </html>
            """;
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}