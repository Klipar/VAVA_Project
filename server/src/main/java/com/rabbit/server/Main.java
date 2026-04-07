package com.rabbit.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.rabbit.server.handler.TaskHandler;
import com.rabbit.server.handler.AiHandler;
import com.rabbit.server.handler.UserHandler;
import com.rabbit.server.repository.UserRepository;
import com.rabbit.server.service.DatabaseService;

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

        // ============ TASK ENDPOINTS ============
        server.createContext("/tasks/", taskHandler.getAll());        // GET  /tasks/{projectId}
        server.createContext("/tasks/create", taskHandler.create());  // POST /tasks/{projectId}/create
        server.createContext("/tasks/update", taskHandler.update());  // PUT  /tasks/{taskId}/update
        server.createContext("/tasks/delete", taskHandler.delete());  // DELETE /tasks/{taskId}/delete

        // ============ AI ENDPOINT ============
        server.createContext("/ai/suggest", aiHandler.suggest());     // POST /ai/suggest

        // ============ USER ENDPOINTS ============
        // Використовуємо один загальний контекст для всіх user ендпоінтів
        server.createContext("/users", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // GET /users/{userId}
                if (path.matches("/users/\\d+$") && method.equals("GET")) {
                    userHandler.getUser().handle(exchange);
                }
                // PUT /users/{userId}/update
                else if (path.matches("/users/\\d+/update$") && method.equals("PUT")) {
                    userHandler.updateUser().handle(exchange);
                }
                // DELETE /users/{userId}/delete
                else if (path.matches("/users/\\d+/delete$") && method.equals("DELETE")) {
                    userHandler.deleteUser().handle(exchange);
                }
                else {
                    send405(exchange);
                }
            } catch (Exception e) {
                e.printStackTrace();
                send500(exchange);
            }
        });

        // ============ PROJECT-USER ENDPOINTS ============
        server.createContext("/projects", (exchange) -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            try {
                // GET /projects/{projectId}/users
                if (path.matches("/projects/\\d+/users$") && method.equals("GET")) {
                    userHandler.getAllUsersFromProject().handle(exchange);
                }
                // POST /projects/{projectId}/users/create
                else if (path.matches("/projects/\\d+/users/create$") && method.equals("POST")) {
                    userHandler.createUser().handle(exchange);
                }
                // POST /projects/{projectId}/users/\\d+/add
                else if (path.matches("/projects/\\d+/users/\\d+/add$") && method.equals("POST")) {
                    userHandler.addUserToProject().handle(exchange);
                }
                // DELETE /projects/{projectId}/users/\\d+/remove
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
        System.out.println("\n=== Available Endpoints ===");
        System.out.println("\nUser Endpoints:");
        System.out.println("  GET    /users/{userId}");
        System.out.println("  PUT    /users/{userId}/update");
        System.out.println("  DELETE /users/{userId}/delete");
        System.out.println("  GET    /projects/{projectId}/users");
        System.out.println("  POST   /projects/{projectId}/users/create");
        System.out.println("  POST   /projects/{projectId}/users/{userId}/add");
        System.out.println("  DELETE /projects/{projectId}/users/{userId}/remove");

        System.out.println("\nTask Endpoints:");
        System.out.println("  GET    /tasks/{projectId}");
        System.out.println("  POST   /tasks/{projectId}/create");
        System.out.println("  PUT    /tasks/{taskId}/update");
        System.out.println("  DELETE /tasks/{taskId}/delete");

        System.out.println("\nAI Endpoint:");
        System.out.println("  POST   /ai/suggest");

        System.out.println("\nDocumentation:");
        System.out.println("  GET    /swagger");
        System.out.println("  GET    /openapi.json");
        System.out.println("  GET    /hello");
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