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
                        },
                        "WorkerDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer"},
                                "name": {"type": "string"},
                                "skills": {
                                    "type": "array",
                                    "items": {"type": "string"}
                                },
                                "active_tasks": {"type": "integer"},
                                "max_tasks": {"type": "integer"},
                                "past_tasks": {
                                    "type": "array",
                                    "items": {"type": "object"}
                                }
                            }
                        },
                        "AiRequestDto": {
                            "type": "object",
                            "required": ["description", "required_skills", "workers"],
                            "properties": {
                                "description": {
                                    "type": "string",
                                    "description": "Task description"
                                },
                                "required_skills": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                    "description": "Skills required for the task"
                                },
                                "workers": {
                                    "type": "array",
                                    "items": {"$ref": "#/components/schemas/WorkerDto"},
                                    "description": "List of available workers"
                                }
                            }
                        },
                        "AiResponseDto": {
                            "type": "object",
                            "properties": {
                                "suggested_worker_id": {"type": "integer"},
                                "suggested_worker_name": {"type": "string"},
                                "reason": {"type": "string"},
                                "confidence_score": {"type": "number", "format": "float"}
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
                    "/ai/suggest": {
                        "post": {
                            "summary": "Get AI suggestion for task assignment",
                            "description": "AI analyzes task requirements and worker skills to suggest the best worker for the job",
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/AiRequestDto"},
                                        "example": {
                                            "description": "fix backend bug",
                                            "required_skills": ["java"],
                                            "workers": [
                                                {
                                                    "id": 1,
                                                    "name": "John",
                                                    "skills": ["java", "python"],
                                                    "active_tasks": 2,
                                                    "max_tasks": 5,
                                                    "past_tasks": []
                                                },
                                                {
                                                    "id": 2,
                                                    "name": "Jane",
                                                    "skills": ["javascript", "react"],
                                                    "active_tasks": 3,
                                                    "max_tasks": 5,
                                                    "past_tasks": []
                                                }
                                            ]
                                        }
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "AI suggestion generated successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/AiResponseDto"},
                                            "example": {
                                                "suggested_worker_id": 1,
                                                "suggested_worker_name": "John",
                                                "reason": "John has required Java skills and has capacity available (2/5 active tasks)",
                                                "confidence_score": 0.95
                                            }
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid input or missing required fields"},
                                "401": {"description": "Unauthorized"},
                                "405": {"description": "Method not allowed - only POST"},
                                "415": {"description": "Unsupported Media Type - expected application/json"},
                                "500": {"description": "Internal server error - AI service unavailable"}
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