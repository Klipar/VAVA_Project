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
                        "name": "Project",
                        "description": "Project management endpoints - create, update, delete projects"
                    },
                    {
                        "name": "Notification",
                        "description": "Notification management endpoints - get and manage user notifications"
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
                            "bearerFormat": "JWT",
                            "description": "JWT token authentication. Enter your token in the format: Bearer <token>"
                        }
                    },
                    "schemas": {
                        "UserDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1, "readOnly": true},
                                "name": {"type": "string", "example": "John Doe"},
                                "nickname": {"type": "string", "example": "johnd"},
                                "email": {"type": "string", "format": "email", "example": "ivan@example.com"},
                                "role": {
                                    "type": "string",
                                    "enum": ["MANAGER", "TEAM_LEADER", "WORKER"],
                                    "example": "TEAM_LEADER"
                                },
                                "createdAt": {"type": "string", "format": "date-time", "example": "2024-01-01T12:00:00Z", "readOnly": true},
                                "skills": {"type": "string", "example": "java,spring,react"}
                            }
                        },
                        "CreateUserRequest": {
                            "type": "object",
                            "required": ["name", "nickname", "email", "password"],
                            "properties": {
                                "name": {"type": "string", "example": "John Doe"},
                                "nickname": {"type": "string", "example": "johnd"},
                                "email": {"type": "string", "format": "email", "example": "ivan@example.com"},
                                "password": {"type": "string", "minLength": 6, "example": "qwerty"},
                                "skills": {"type": "string", "example": "java,spring"}
                            }
                        },
                        "UpdateUserRequest": {
                            "type": "object",
                            "properties": {
                                "name": {"type": "string", "example": "John Updated"},
                                "nickname": {"type": "string", "example": "johnu"},
                                "email": {"type": "string", "format": "email", "example": "john.updated@example.com"},
                                "skills": {"type": "string", "example": "java,spring,python"}
                            }
                        },
                        "LoginRequest": {
                            "type": "object",
                            "required": ["email", "password"],
                            "properties": {
                                "email": {
                                    "type": "string",
                                    "format": "email",
                                    "description": "User's email address",
                                    "example": "ivan@example.com"
                                },
                                "password": {
                                    "type": "string",
                                    "description": "User's password",
                                    "minLength": 6,
                                    "example": "qwerty"
                                }
                            }
                        },
                        "SuccessAuthDto": {
                            "type": "object",
                            "properties": {
                                "token": {
                                    "type": "string",
                                    "description": "JWT authentication token",
                                    "example": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                                },
                                "user": {
                                    "$ref": "#/components/schemas/UserDto",
                                    "description": "User information"
                                }
                            }
                        },
                        "ProjectDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1, "readOnly": true},
                                "title": {"type": "string", "example": "Project Alpha"},
                                "description": {"type": "string", "example": "Main development project"},
                                "deadline": {"type": "string", "format": "date-time", "example": "2024-12-31T23:59:59Z"},
                                "status": {
                                    "type": "string",
                                    "enum": ["active", "completed", "archived"],
                                    "example": "active"
                                },
                                "masterId": {"type": "integer", "format": "int64", "example": 1, "readOnly": true}
                            }
                        },
                        "NotificationDto": {
                            "type": "object",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1},
                                "message": {"type": "string", "example": "You have been assigned to task #123"},
                                "createdAt": {"type": "string", "format": "date-time", "example": "2024-01-01T12:00:00Z"},
                                "isRead": {"type": "boolean", "example": false}
                            }
                        },
                        "CreateNotificationRequest": {
                            "type": "object",
                            "required": ["message"],
                            "properties": {
                                "message": {"type": "string", "example": "Task deadline approaching"}
                            }
                        },
                        "TaskDto": {
                            "type": "object",
                            "description": "Full task representation returned by the server",
                            "properties": {
                                "id": {"type": "integer", "format": "int64", "example": 1, "readOnly": true},
                                "projectId": {"type": "integer", "format": "int64", "example": 1, "readOnly": true},
                                "assignedTo": {"type": "integer", "format": "int64", "example": 2},
                                "createdBy": {"type": "integer", "format": "int64", "example": 1, "readOnly": true},
                                "title": {"type": "string", "example": "Fix login bug"},
                                "description": {"type": "string", "example": "Users cannot login with correct credentials"},
                                "priority": {"type": "integer", "minimum": 0, "maximum": 4, "example": 2},
                                "status": {"type": "string", "enum": ["pending", "in_progress", "completed", "cancelled"], "example": "in_progress"},
                                "deadline": {"type": "string", "format": "date-time", "example": "2024-12-31T23:59:59Z"}
                            }
                        },
                        "TaskResponseWithMessage": {
                            "type": "object",
                            "description": "Response wrapper for task operations that includes both message and task data",
                            "properties": {
                                "message": {
                                    "type": "string",
                                    "example": "Task created successfully"
                                },
                                "task": {
                                    "$ref": "#/components/schemas/TaskDto"
                                }
                            }
                        },
                        "TaskRequestDto": {
                            "type": "object",
                            "description": "Request body for creating or updating a task. IMPORTANT: Do NOT include 'id', 'projectId', or 'createdBy' fields - they are automatically set by the server.",
                            "required": ["title", "priority", "status"],
                            "properties": {
                                "assignedTo": {
                                    "type": "integer",
                                    "format": "int64",
                                    "description": "ID of the user assigned to the task",
                                    "example": 2
                                },
                                "title": {
                                    "type": "string",
                                    "description": "Task title",
                                    "example": "Fix login bug"
                                },
                                "description": {
                                    "type": "string",
                                    "description": "Detailed task description",
                                    "example": "Users cannot login with correct credentials"
                                },
                                "priority": {
                                    "type": "integer",
                                    "minimum": 0,
                                    "maximum": 4,
                                    "description": "Task priority (0=Lowest, 4=Highest)",
                                    "example": 2
                                },
                                "status": {
                                    "type": "string",
                                    "enum": ["pending", "in_progress", "completed", "cancelled"],
                                    "description": "Current task status",
                                    "example": "in_progress"
                                },
                                "deadline": {
                                    "type": "string",
                                    "format": "date-time",
                                    "description": "Task deadline in ISO format (YYYY-MM-DDTHH:MM:SSZ)",
                                    "example": "2024-12-31T23:59:59Z"
                                }
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
                        },
                        "MessageResponse": {
                            "type": "object",
                            "properties": {
                                "message": {"type": "string", "example": "Operation completed successfully"}
                            }
                        }
                    }
                },
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
                    "/users/login": {
                        "post": {
                            "tags": ["User"],
                            "summary": "User login",
                            "description": "Authenticate user with email and password to receive JWT token",
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/LoginRequest"}
                                    }
                                }
                            },
                            "responses": {
                                "201": {
                                    "description": "Login successful - returns JWT token and user info",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/SuccessAuthDto"}
                                        }
                                    }
                                },
                                "400": {
                                    "description": "Bad request - invalid input format or missing required fields",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "401": {
                                    "description": "Invalid credentials - email or password is incorrect",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "405": {"description": "Method not allowed - only POST method is supported"},
                                "500": {
                                    "description": "Internal server error",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "/users/{userId}": {
                        "get": {
                            "tags": ["User"],
                            "summary": "Get user by ID",
                            "description": "Retrieve detailed information about a specific user",
                            "security": [{"bearerAuth": []}],
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
                                "403": {"description": "Forbidden - insufficient permissions"},
                                "404": {"description": "User not found"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/users/nickname/{nickname}": {
                        "get": {
                            "tags": ["User"],
                            "summary": "Get user by nickname",
                            "description": "Retrieve detailed information about a specific user by their nickname",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "nickname",
                                "in": "path",
                                "required": true,
                                "description": "Nickname of the user to retrieve",
                                "schema": {"type": "string", "example": "ivan"}
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
                                "403": {"description": "Forbidden - insufficient permissions"},
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
                            "security": [{"bearerAuth": []}],
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
                            "security": [{"bearerAuth": []}],
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
                    "/projects": {
                        "get": {
                            "tags": ["Project"],
                            "summary": "Get all projects for current user",
                            "description": "Retrieve list of projects where the authenticated user is a member",
                            "security": [{"bearerAuth": []}],
                            "responses": {
                                "200": {
                                    "description": "List of projects",
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "type": "array",
                                                "items": {"$ref": "#/components/schemas/ProjectDto"}
                                            }
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "500": {"description": "Internal server error"}
                            }
                        },
                        "post": {
                            "tags": ["Project"],
                            "summary": "Create a new project",
                            "description": "Create a new project (authenticated users only). User becomes project master.",
                            "security": [{"bearerAuth": []}],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/ProjectDto"}
                                    }
                                }
                            },
                            "responses": {
                                "201": {
                                    "description": "Project created successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ProjectDto"}
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid input"},
                                "401": {"description": "Unauthorized"},
                                "500": {"description": "Internal server error"}
                            }
                        }
                    },
                    "/projects/{projectId}": {
                        "get": {
                            "tags": ["Project"],
                            "summary": "Get project by ID",
                            "description": "Retrieve detailed information about a specific project (only members)",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project to retrieve",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "Project found successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ProjectDto"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - not a member of this project"},
                                "404": {"description": "Project not found"},
                                "500": {"description": "Internal server error"}
                            }
                        },
                        "put": {
                            "tags": ["Project"],
                            "summary": "Update project",
                            "description": "Update an existing project (only project master)",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project to update",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/ProjectDto"}
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "Project updated successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ProjectDto"}
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid input"},
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only project master can update"},
                                "404": {"description": "Project not found"},
                                "500": {"description": "Internal server error"}
                            }
                        },
                        "delete": {
                            "tags": ["Project"],
                            "summary": "Delete project",
                            "description": "Delete a project (only project master)",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "projectId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the project to delete",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "Project deleted successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/MessageResponse"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "403": {"description": "Forbidden - only project master can delete"},
                                "404": {"description": "Project not found"},
                                "500": {"description": "Internal server error"}
                            }
                        }
                    },
                    "/projects/{projectId}/users": {
                        "get": {
                            "tags": ["User"],
                            "summary": "Get all users from project",
                            "description": "Retrieve list of all users belonging to a specific project",
                            "security": [{"bearerAuth": []}],
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
                                "403": {"description": "Forbidden - insufficient permissions"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/projects/{projectId}/users/create": {
                        "post": {
                            "tags": ["User"],
                            "summary": "Create a new user",
                            "description": "Create a new user in the project (Manager creates WORKER, Team Leader creates TEAM_LEADER)",
                            "security": [{"bearerAuth": []}],
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
                            "security": [{"bearerAuth": []}],
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
                            "security": [{"bearerAuth": []}],
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
                    "/notifications": {
                        "get": {
                            "tags": ["Notification"],
                            "summary": "Get all notifications for current user",
                            "description": "Retrieve all notifications for the authenticated user",
                            "security": [{"bearerAuth": []}],
                            "responses": {
                                "200": {
                                    "description": "List of notifications",
                                    "content": {
                                        "application/json": {
                                            "schema": {
                                                "type": "array",
                                                "items": {"$ref": "#/components/schemas/NotificationDto"}
                                            }
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "500": {"description": "Internal server error"}
                            }
                        },
                        "post": {
                            "tags": ["Notification"],
                            "summary": "Create a notification for current user",
                            "description": "Create a new notification (automatically linked to authenticated user)",
                            "security": [{"bearerAuth": []}],
                            "requestBody": {
                                "required": true,
                                "content": {
                                    "application/json": {
                                        "schema": {"$ref": "#/components/schemas/CreateNotificationRequest"}
                                    }
                                }
                            },
                            "responses": {
                                "201": {
                                    "description": "Notification created successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/NotificationDto"}
                                        }
                                    }
                                },
                                "400": {"description": "Bad request - invalid input"},
                                "401": {"description": "Unauthorized"},
                                "500": {"description": "Internal server error"}
                            }
                        }
                    },
                    "/notifications/{notificationId}/read": {
                        "put": {
                            "tags": ["Notification"],
                            "summary": "Mark notification as read",
                            "description": "Mark a specific notification as read for the authenticated user",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "notificationId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the notification to mark as read",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "Notification marked as read successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/MessageResponse"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized"},
                                "404": {"description": "Notification not found"},
                                "500": {"description": "Internal server error"}
                            }
                        }
                    },
                    "/tasks/{projectId}": {
                        "get": {
                            "tags": ["Task"],
                            "summary": "Get all tasks for a project",
                            "description": "Retrieve list of all tasks in a specific project. User must be a member of the project.",
                            "security": [{"bearerAuth": []}],
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
                                "403": {"description": "Forbidden - you are not a member of this project"},
                                "405": {"description": "Method not allowed"}
                            }
                        }
                    },
                    "/tasks/{projectId}/create": {
                        "post": {
                            "tags": ["Task"],
                            "summary": "Create a new task",
                            "description": "Create a new task in a project (Only project admin can create tasks).\\n\\n**IMPORTANT:** Do NOT include 'id', 'projectId', or 'createdBy' in the request body - these are automatically set by the server.",
                            "security": [{"bearerAuth": []}],
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
                                        "schema": {"$ref": "#/components/schemas/TaskRequestDto"},
                                        "example": {
                                            "title": "Fix login bug",
                                            "description": "Users cannot login with correct credentials",
                                            "priority": 2,
                                            "status": "in_progress",
                                            "assignedTo": 2,
                                            "deadline": "2024-12-31T23:59:59Z"
                                        }
                                    }
                                }
                            },
                            "responses": {
                                "201": {
                                    "description": "Task created successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/TaskResponseWithMessage"}
                                        }
                                    }
                                },
                                "400": {
                                    "description": "Bad request - invalid input or missing required fields",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized - Bearer token required"},
                                "403": {
                                    "description": "Forbidden - only project admin can create tasks",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "405": {"description": "Method not allowed - only POST method is supported"},
                                "500": {
                                    "description": "Internal server error",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "/tasks/{taskId}/update": {
                        "put": {
                            "tags": ["Task"],
                            "summary": "Update a task",
                            "description": "Update an existing task (Only project admin can update tasks).\\n\\n**IMPORTANT:** Do NOT include 'id', 'projectId', or 'createdBy' in the request body - these fields are read-only.",
                            "security": [{"bearerAuth": []}],
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
                                        "schema": {"$ref": "#/components/schemas/TaskRequestDto"},
                                        "example": {
                                            "title": "Fix login bug - UPDATED",
                                            "description": "Users cannot login with correct credentials - needs immediate fix",
                                            "priority": 3,
                                            "status": "in_progress",
                                            "assignedTo": 3,
                                            "deadline": "2024-12-15T23:59:59Z"
                                        }
                                    }
                                }
                            },
                            "responses": {
                                "200": {
                                    "description": "Task updated successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/TaskResponseWithMessage"}
                                        }
                                    }
                                },
                                "400": {
                                    "description": "Bad request - invalid input",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized - Bearer token required"},
                                "403": {
                                    "description": "Forbidden - only project admin can update tasks",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "404": {
                                    "description": "Task not found",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "405": {"description": "Method not allowed - only PUT method is supported"},
                                "500": {
                                    "description": "Internal server error",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "/tasks/{taskId}/delete": {
                        "delete": {
                            "tags": ["Task"],
                            "summary": "Delete a task",
                            "description": "Delete a task (Only project admin can delete tasks)",
                            "security": [{"bearerAuth": []}],
                            "parameters": [{
                                "name": "taskId",
                                "in": "path",
                                "required": true,
                                "description": "ID of the task to delete",
                                "schema": {"type": "integer", "format": "int64", "example": 1}
                            }],
                            "responses": {
                                "200": {
                                    "description": "Task deleted successfully",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/TaskResponseWithMessage"}
                                        }
                                    }
                                },
                                "401": {"description": "Unauthorized - Bearer token required"},
                                "403": {
                                    "description": "Forbidden - only project admin can delete tasks",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "404": {
                                    "description": "Task not found",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                },
                                "405": {"description": "Method not allowed - only DELETE method is supported"},
                                "500": {
                                    "description": "Internal server error",
                                    "content": {
                                        "application/json": {
                                            "schema": {"$ref": "#/components/schemas/ErrorResponse"}
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "/ai/suggest": {
                        "post": {
                            "tags": ["AI"],
                            "summary": "Get AI suggestion for task assignment",
                            "description": "AI analyzes task requirements and worker skills to suggest the best worker for the job",
                            "security": [{"bearerAuth": []}],
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
                                "403": {"description": "Forbidden - insufficient permissions"},
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