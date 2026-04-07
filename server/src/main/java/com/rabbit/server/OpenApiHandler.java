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
                    "version": "1.0.0",
                    "description": "API for managing projects, tasks, and users",
                    "contact": {
                        "name": "Rabbit Team",
                        "email": "support@rabbit.com"
                    }
                },
                "servers": [{"url": "http://localhost:6969"}],
                "tags": [
                    {
                        "name": "User",
                        "description": "User management endpoints - create, update, delete users"
                    },
                    {
                        "name": "Task",
                        "description": "Task management endpoints - create, update, delete tasks"
                    },
                    {
                        "name": "AI",
                        "description": "AI-powered task assignment suggestions"
                    },
                    {
                        "name": "System",
                        "description": "System health and documentation endpoints"
                    }
                ],
                "components": {
                    "securitySchemes": {
                        "bearerAuth": {
                            "type": "http",
                            "scheme": "bearer",
                            "description": "JWT token authentication"
                        }
                    },
                    "schemas": {
                        "UserDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1},
                                "name": {"type": "string", "example": "John Doe"},
                                "nickname": {"type": "string", "example": "johnd"},
                                "email": {"type": "string", "format": "email", "example": "john@example.com"},
                                "role": {
                                    "type": "string",
                                    "enum": ["MANAGER", "TEAM_LEADER", "WORKER"],
                                    "example": "TEAM_LEADER"
                                },
                                "createdAt": {"type": "string", "format": "date-time", "example": "2024-01-01T12:00:00Z"}
                            }
                        },
                        "CreateUserRequest": {
                            "type": "object",
                            "required": ["name", "nickname", "email", "password"],
                            "properties": {
                                "name": {"type": "string", "example": "John Doe"},
                                "nickname": {"type": "string", "example": "johnd"},
                                "email": {"type": "string", "format": "email", "example": "john@example.com"},
                                "password": {"type": "string", "minLength": 6, "example": "securepassword123"}
                            }
                        },
                        "UpdateUserRequest": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string", "example": "John Updated"},
                                "nickname": {"type": "string", "example": "johnu"},
                                "email": {"type": "string", "format": "email", "example": "john.updated@example.com"}
                            }
                        },
                        "TaskDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1},
                                "projectId": {"type": "integer", "format": "int64", "example": 1},
                                "assignedTo": {"type": "integer", "format": "int64", "example": 2},
                                "createdBy": {"type": "integer", "format": "int64", "example": 1},
                                "title": {"type": "string", "example": "Fix login bug"},
                                "description": {"type": "string", "example": "Users cannot login with correct credentials"},
                                "priority": {"type": "integer", "minimum": 0, "maximum": 4, "example": 2},
                                "status": {"type": "string", "example": "in_progress"},
                                "deadline": {"type": "string", "format": "date-time", "example": "2024-12-31T23:59:59Z"}
                            }
                        },
                        "WorkerDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "example": 1},
                                "name": {"type": "string", "example": "John"},
                                "skills": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                    "example": ["java", "python", "sql"]
                                },
                                "active_tasks": {"type": "integer", "example": 2},
                                "max_tasks": {"type": "integer", "example": 5},
                                "past_tasks": {
                                    "type": "array",
                                    "items": {"type": "object"},
                                    "example": []
                                }
                            }
                        },
                        "AiRequestDto": {
                            "type": "object",
                            "required": ["description", "required_skills", "workers"],
                            "properties": {
                                "description": {
                                    "type": "string",
                                    "description": "Task description",
                                    "example": "Fix backend authentication bug"
                                },
                                "required_skills": {
                                    "type": "array",
                                    "items": {"type": "string"},
                                    "description": "Skills required for the task",
                                    "example": ["java", "spring", "security"]
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
                                "suggested_worker_id": {"type": "integer", "example": 1},
                                "suggested_worker_name": {"type": "string", "example": "John"},
                                "reason": {"type": "string", "example": "John has required Java skills and has capacity available (2/5 active tasks)"},
                                "confidence_score": {"type": "number", "format": "float", "example": 0.95}
                            }
                        },
                        "ErrorResponse": {
                            "type": "object",
                            "properties": {
                                "error": {"type": "string", "example": "Error message"}
                            }
                        }
                    }
                },
                "security": [{"bearerAuth": []}],
                "paths": {
                    "/hello": {
                        "get": {
                            "tags": ["System"],
                            "summary": "Health check endpoint",
                            "description": "Simple endpoint to check if the server is running",
                            "responses": {
                                "200": {"description": "Server is running"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/swagger": {
                        "get": {
                            "tags": ["System"],
                            "summary": "Swagger UI",
                            "description": "Interactive API documentation",
                            "responses": {
                                "200": {"description": "Swagger UI HTML page"}
                            }
                        }
                    },
                    "/openapi.json": {
                        "get": {
                            "tags": ["System"],
                            "summary": "OpenAPI specification",
                            "description": "OpenAPI JSON specification",
                            "responses": {
                                "200": {"description": "OpenAPI JSON"}
                            }
                        }
                    },
                    "/users/{userId}": {
                        "get": {
                            "tags": ["User"],
                            "summary": "Get user by ID",
                            "description": "Retrieve detailed information about a specific user",
                            "parameters": [{
                                "name": "userId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the user to retrieve",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "User found successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/UserDto"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized - Bearer token required"},
                                "404": {"description": "User not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/users/{userId}/update": {
                        "put": {
                            "tags": ["User"],
                            "summary": "Update user",
                            "description": "Update user information (only self)",
                            "parameters": [{
                                "name": "userId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the user to update",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/UpdateUserRequest"}
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "User updated successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/UserDto"}
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid data or email already exists"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - can only update your own account"},
                                "404": {"description": "User not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/users/{userId}/delete": {
                        "delete": {
                            "tags": ["User"],
                            "summary": "Delete user",
                            "description": "Delete user account (only self)",
                            "parameters": [{
                                "name": "userId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the user to delete",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {"description": "User deleted successfully"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - can only delete your own account"},
                                "404": {"description": "User not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/projects/{projectId}/users": {
                        "get": {
                            "tags": ["User"],
                            "summary": "Get all users from project",
                            "description": "Retrieve list of all users belonging to a specific project",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "List of users in project",
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "type": "array",
                                                "items": {"$ref": "#/components/schemas/UserDto"}
                                            }
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/projects/{projectId}/users/create": {
                        "post": {
                            "tags": ["User"],
                            "summary": "Create a new user",
                            "description": "Create a new user in the project (Manager creates WORKER, Team Leader creates TEAM_LEADER)",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/CreateUserRequest"}
                                    }
                                }
                            },
                            "responses": {
                                "201": {
                                    "description": "User created successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/UserDto"}
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid data or email already exists"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only Manager or Team Leader can create users"},
                                "405": {"description": "Method not allowed"},
                                "500": {"description": "Internal server error"}
                            }
                        }
                    },
                    "/projects/{projectId}/users/{userId}/add": {
                        "post": {
                            "tags": ["User"],
                            "summary": "Add user to project",
                            "description": "Add an existing user to a project (Manager or Team Leader only)",
                            "parameters": [
                                {
                                    "name": "projectId",
                                    "in": "path",
                                    "required": true,
                                    "description": "ID of the project",
                                    "schema": {"type": "integer", "format": "int64", "example": 1}
                                },
                                {
                                    "name": "userId",
                                    "in": "path",
                                    "required": true,
                                    "description": "ID of the user to add",
                                    "schema": {"type": "integer", "format": "int64", "example": 2}
                                }
                            ],
                            "responses": {
                                "200": {"description": "User added to project successfully"},
                                "400": {"description": "Bad request - user already in project"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only Manager or Team Leader can add users"},
                                "404": {"description": "User or project not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/projects/{projectId}/users/{userId}/remove": {
                        "delete": {
                            "tags": ["User"],
                            "summary": "Remove user from project",
                            "description": "Remove a user from a project (only self)",
                            "parameters": [
                                {
                                    "name": "projectId",
                                    "in": "path",
                                    "required": true,
                                    "description": "ID of the project",
                                    "schema": {"type": "integer", "format": "int64", "example": 1}
                                },
                                {
                                    "name": "userId",
                                    "in": "path",
                                    "required": true,
                                    "description": "ID of the user to remove",
                                    "schema": {"type": "integer", "format": "int64", "example": 2}
                                }
                            ],
                            "responses": {
                                "200": {"description": "User removed from project successfully"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - can only remove yourself from project"},
                                "404": {"description": "User or project not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/tasks/{projectId}": {
                        "get": {
                            "tags": ["Task"],
                            "summary": "Get all tasks for a project",
                            "description": "Retrieve list of all tasks in a specific project",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
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
                                "401": {"description": "Unauthorized"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/tasks/{projectId}/create": {
                        "post": {
                            "tags": ["Task"],
                            "summary": "Create a new task",
                            "description": "Create a new task in a project (Only Manager or Team Leader can create tasks)",
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
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
                                "201": {"description": "Task created successfully"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only Manager or Team Leader can create tasks"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/tasks/{taskId}/update": {
                        "put": {
                            "tags": ["Task"],
                            "summary": "Update a task",
                            "description": "Update an existing task (Only Manager or Team Leader can update tasks)",
                            "parameters": [{
                                "name": "taskId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the task to update",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
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
                                "200": {"description": "Task updated successfully"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only Manager or Team Leader can update tasks"},
                                "404": {"description": "Task not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/tasks/{taskId}/delete": {
                        "delete": {
                            "tags": ["Task"],
                            "summary": "Delete a task",
                            "description": "Delete a task (Only Manager or Team Leader can delete tasks)",
                            "parameters": [{
                                "name": "taskId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the task to delete",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {"description": "Task deleted successfully"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only Manager or Team Leader can delete tasks"},
                                "404": {"description": "Task not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/ai/suggest": {
                        "post": {
                            "tags": ["AI"],
                            "summary": "Get AI suggestion for task assignment",
                            "description": "AI analyzes task requirements and worker skills to suggest the best worker for the job",
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/AiRequestDto"}
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "AI suggestion generated successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/AiResponseDto"}
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