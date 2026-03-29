package com.rabbit.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class OpenApiHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String spec = """
            {
                "openapi": "3.0.0",
                "info": {
                    "title": "Rabbit API",
                    "version": "1.0.0"
                },
                "servers": [{"url": "http://localhost:6969"}],
                "components": {
                    "securitySchemes": {
                        "bearerAuth": {
                            "type": "http",
                            "scheme": "bearer"
                        }
                    },
                    "schemas": {
                        "TaskDto": {
                            "type": "object",
                            "properties": {
                                "id":          {"type": "integer"},
                                "projectId":   {"type": "integer"},
                                "assignedTo":  {"type": "integer"},
                                "createdBy":   {"type": "integer"},
                                "title":       {"type": "string"},
                                "description": {"type": "string"},
                                "priority":    {"type": "integer"},
                                "status":      {"type": "string"},
                                "deadline":    {"type": "string", "format": "date-time"}
                            }
                        }
                    }
                },
                "security": [{"bearerAuth": []}],
                "paths": {
                    "/hello": {
                        "get": {
                            "summary": "Hello endpoint",
                            "responses": {
                                "200": {"description": "Success"}
                            }
                        }
                    },
                    "/tasks/{projectId}": {
                        "get": {
                            "summary": "Get all tasks for a project",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "schema": {"type": "integer"}
                            }],
                            "responses": {
                                "200": {
                                    "description": "List of tasks",
                                    "content": {
                                        "application/json": {
                                            "schema": {"type": "array", "items": {"$ref": "#/components/schemas/TaskDto"}}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"}
                            }
                        }
                    },
                    "/tasks/{projectId}/create": {
                        "post": {
                            "summary": "Create a new task (admin only)",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "schema": {"type": "integer"}
                            }],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/TaskDto"}
                                    }
                                }
                            },
                            "responses": {
                                "201": {"description": "Task created"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - admin only"}
                            }
                        }
                    },
                    "/tasks/{taskId}/update": {
                        "put": {
                            "summary": "Update a task (admin only)",
                            "parameters": [{
                                "name": "taskId",
                                "in": "path",
                                "required": true,
                                "schema": {"type": "integer"}
                            }],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/TaskDto"}
                                    }
                                }
                            },
                            "responses": {
                                "200": {"description": "Updated"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - admin only"},
                                "404": {"description": "Task not found"}
                            }
                        }
                    },
                    "/tasks/{taskId}/delete": {
                        "delete": {
                            "summary": "Delete a task (admin only)",
                            "parameters": [{
                                "name": "taskId",
                                "in": "path",
                                "required": true,
                                "schema": {"type": "integer"}
                            }],
                            "responses": {
                                "200": {"description": "Deleted"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - admin only"},
                                "404": {"description": "Task not found"}
                            }
                        }
                    }
                }
            }
        """;

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, spec.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(spec.getBytes());
        }
    }
}
