package com.diversitus.plugins

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*

fun Application.configureOpenAPI() {
    routing {
        // Serve OpenAPI JSON specification
        get("/openapi.json") {
            call.respondText(
                contentType = ContentType.Application.Json,
                text = openApiSpec
            )
        }

        // Serve Swagger UI
        get("/swagger-ui") {
            call.respondText(
                contentType = ContentType.Text.Html,
                text = swaggerHtml
            )
        }

        // API documentation endpoint
        get("/docs") {
            call.respondText(
                contentType = ContentType.Text.Html,
                text = swaggerHtml
            )
        }
    }
}

private val openApiSpec = """
{
  "openapi": "3.0.0",
  "info": {
    "title": "Diversitus API",
    "version": "1.0.0",
    "description": "Job matching and company management API for neurodiversity support"
  },
  "servers": [
    {
      "url": "https://api.diversitus.uk",
      "description": "Production server"
    },
    {
      "url": "http://localhost:8080",
      "description": "Development server"
    }
  ],
  "paths": {
    "/": {
      "get": {
        "summary": "Welcome message",
        "description": "Returns a welcome message to confirm the API is running",
        "responses": {
          "200": {
            "description": "Welcome message",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string",
                  "example": "Welcome to the Diversitus API!"
                }
              }
            }
          }
        }
      }
    },
    "/health": {
      "get": {
        "summary": "Health check",
        "description": "Returns the health status of the service",
        "responses": {
          "200": {
            "description": "Service health status",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "status": {
                      "type": "string",
                      "example": "UP"
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "/jobs": {
      "get": {
        "summary": "Get jobs",
        "description": "Retrieves job listings. Returns all jobs if no companyId is provided, or jobs for a specific company if companyId is provided.",
        "tags": ["Jobs"],
        "parameters": [
          {
            "name": "companyId",
            "in": "query",
            "required": false,
            "schema": {
              "type": "string"
            },
            "description": "Optional: Filter jobs by company ID"
          }
        ],
        "responses": {
          "200": {
            "description": "List of jobs (filtered by companyId if provided)",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "string"},
                      "companyId": {"type": "string"},
                      "title": {"type": "string"},
                      "description": {"type": "string"},
                      "traits": {
                        "type": "object",
                        "additionalProperties": {"type": "integer"}
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "summary": "Create or update job",
        "description": "Creates a new job or updates an existing one. ID is auto-generated if not provided.",
        "tags": ["Jobs"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "id": {"type": "string", "description": "Optional: Job ID (auto-generated if not provided)"},
                  "companyId": {"type": "string", "description": "Required: Company ID this job belongs to"},
                  "title": {"type": "string", "description": "Job title"},
                  "description": {"type": "string", "description": "Job description"},
                  "traits": {
                    "type": "object",
                    "additionalProperties": {"type": "integer"},
                    "description": "Job-specific trait requirements"
                  }
                },
                "required": ["companyId", "title", "description"]
              }
            }
          }
        },
        "responses": {
          "201": {
            "description": "Job created or updated successfully",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"},
                    "companyId": {"type": "string"},
                    "title": {"type": "string"},
                    "description": {"type": "string"},
                    "traits": {
                      "type": "object",
                      "additionalProperties": {"type": "integer"}
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "/companies": {
      "get": {
        "summary": "Get all companies",
        "description": "Retrieves a list of all partner companies",
        "tags": ["Companies"],
        "responses": {
          "200": {
            "description": "List of all companies",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "string"},
                      "name": {"type": "string"},
                      "email": {"type": "string"},
                      "traits": {
                        "type": "object",
                        "additionalProperties": {"type": "integer"}
                      }
                    }
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "summary": "Create or update company",
        "description": "Creates a new company or updates an existing one. ID is auto-generated if not provided.",
        "tags": ["Companies"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "id": {"type": "string", "description": "Optional: Company ID (auto-generated if not provided)"},
                  "name": {"type": "string", "description": "Company name"},
                  "email": {"type": "string", "description": "Company email address"},
                  "traits": {
                    "type": "object",
                    "additionalProperties": {"type": "integer"},
                    "description": "Company trait scores"
                  }
                },
                "required": ["name", "email"]
              }
            }
          }
        },
        "responses": {
          "201": {
            "description": "Company created or updated successfully",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"},
                    "name": {"type": "string"},
                    "email": {"type": "string"},
                    "traits": {
                      "type": "object",
                      "additionalProperties": {"type": "integer"}
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "/companies/{id}": {
      "get": {
        "summary": "Get company by ID or email",
        "description": "Retrieves a company by their ID or email address",
        "tags": ["Companies"],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            },
            "description": "Company ID or email address"
          }
        ],
        "responses": {
          "200": {
            "description": "Company found",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"},
                    "name": {"type": "string"},
                    "email": {"type": "string"},
                    "traits": {
                      "type": "object",
                      "additionalProperties": {"type": "integer"}
                    }
                  }
                }
              }
            }
          },
          "400": {
            "description": "Missing or invalid ID/email"
          },
          "404": {
            "description": "Company not found"
          }
        }
      }
    },
    "/auth/company/login": {
      "post": {
        "summary": "Company login",
        "description": "Authenticates a company using email address",
        "tags": ["Authentication"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "email": {
                    "type": "string",
                    "example": "company@example.com"
                  }
                },
                "required": ["email"]
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "Login successful",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "success": {"type": "boolean"},
                    "company": {
                      "type": "object",
                      "properties": {
                        "id": {"type": "string"},
                        "name": {"type": "string"},
                        "email": {"type": "string"},
                        "traits": {
                          "type": "object",
                          "additionalProperties": {"type": "integer"}
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "404": {
            "description": "Company not found",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"}
                  }
                }
              }
            }
          }
        }
      }
    },
    "/auth/user/login": {
      "post": {
        "summary": "User login",
        "description": "Authenticates a user using email address",
        "tags": ["Authentication"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "email": {
                    "type": "string",
                    "example": "user@example.com"
                  }
                },
                "required": ["email"]
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "Login successful",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "success": {"type": "boolean"},
                    "user": {
                      "type": "object",
                      "properties": {
                        "id": {"type": "string"},
                        "name": {"type": "string"},
                        "email": {"type": "string"},
                        "profile": {
                          "type": "object",
                          "properties": {
                            "traits": {
                              "type": "object",
                              "additionalProperties": {"type": "integer"}
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "404": {
            "description": "User not found",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "success": {"type": "boolean"},
                    "message": {"type": "string"}
                  }
                }
              }
            }
          }
        }
      }
    },
    "/users": {
      "post": {
        "summary": "Create or update user",
        "description": "Creates a new user or updates an existing one. ID is auto-generated if not provided.",
        "tags": ["Users"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "id": {"type": "string", "description": "Optional: User ID (auto-generated if not provided)"},
                  "name": {"type": "string", "description": "User name"},
                  "email": {"type": "string", "description": "User email address"},
                  "profile": {
                    "type": "object",
                    "properties": {
                      "traits": {
                        "type": "object",
                        "additionalProperties": {"type": "integer"},
                        "description": "User neurodiversity trait scores"
                      }
                    }
                  }
                },
                "required": ["name", "email", "profile"]
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "User created or updated successfully",
            "content": {
              "application/json": {
                "schema": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "string"},
                    "name": {"type": "string"},
                    "email": {"type": "string"},
                    "profile": {
                      "type": "object",
                      "properties": {
                        "traits": {
                          "type": "object",
                          "additionalProperties": {"type": "integer"}
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    },
    "/users/{id}": {
      "get": {
        "summary": "Get user by ID or email",
        "description": "Retrieves a user by their ID or email address",
        "tags": ["Users"],
        "parameters": [
          {
            "name": "id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            },
            "description": "User ID or email address"
          }
        ],
        "responses": {
          "200": {
            "description": "User found"
          },
          "400": {
            "description": "Missing or invalid ID/email"
          },
          "404": {
            "description": "User not found"
          }
        }
      }
    },
    "/match": {
      "post": {
        "summary": "Find matching jobs",
        "description": "Finds jobs that match a given neurodiversity profile",
        "tags": ["Matching"],
        "requestBody": {
          "required": true,
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "properties": {
                  "traits": {
                    "type": "object",
                    "additionalProperties": {"type": "integer"},
                    "example": {
                      "attention_to_detail": 8,
                      "problem_solving": 9,
                      "working_from_home": 7
                    }
                  }
                }
              }
            }
          }
        },
        "responses": {
          "200": {
            "description": "List of matching jobs sorted by compatibility",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "string"},
                      "title": {"type": "string"},
                      "description": {"type": "string"},
                      "companyId": {"type": "string"},
                      "traits": {
                        "type": "object",
                        "additionalProperties": {"type": "integer"}
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  },
  "tags": [
    {
      "name": "Jobs",
      "description": "Job listings management"
    },
    {
      "name": "Companies",
      "description": "Company management"
    },
    {
      "name": "Users",
      "description": "User management"
    },
    {
      "name": "Matching",
      "description": "Job matching algorithms"
    },
    {
      "name": "Authentication",
      "description": "Authentication and authorization endpoints"
    }
  ]
}
""".trimIndent()

private val swaggerHtml = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Diversitus API Documentation</title>
    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui.css" />
</head>
<body>
    <div id="swagger-ui"></div>
    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-bundle.js"></script>
    <script src="https://unpkg.com/swagger-ui-dist@5.10.3/swagger-ui-standalone-preset.js"></script>
    <script>
        window.onload = function() {
            const ui = SwaggerUIBundle({
                url: '/openapi.json',
                dom_id: '#swagger-ui',
                deepLinking: true,
                presets: [
                    SwaggerUIBundle.presets.apis,
                    SwaggerUIStandalonePreset
                ],
                plugins: [
                    SwaggerUIBundle.plugins.DownloadUrl
                ],
                layout: "StandaloneLayout"
            });
        };
    </script>
</body>
</html>
""".trimIndent()