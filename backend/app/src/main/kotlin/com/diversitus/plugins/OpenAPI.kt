package com.diversitus.plugins

import io.github.smiley4.ktoropenapi.OpenApi
import io.github.smiley4.ktoropenapi.openApi
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.util.pipeline.*

fun Application.configureOpenAPI() {
    install(OpenApi) {
        info {
            title = "Diversitus API"
            version = "1.0.0"
            description = "Job matching and company management API"
        }
        // You can optionally specify a server
        // server("http://localhost:8080") {
        //     description = "Development server"
        // }
    }

    routing {
        // Serve OpenAPI JSON
        route("/openapi.json") {
            openApi()
        }

        // Alternative endpoint
        route("/api.json") {
            openApi()
        }

        // Serve Swagger UI
        get("/swagger-ui") {
            call.respondText(
                text = swaggerUI("/openapi.json"),
                contentType = ContentType.Text.Html
            )
        }
    }
}

private fun swaggerUI(specUrl: String) = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Diversitus API Documentation</title>
        <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css" />
    </head>
    <body>
        <div id="swagger-ui"></div>
        <script src="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js"></script>
        <script src="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-standalone-preset.js"></script>
        <script>
            window.onload = function() {
                const ui = SwaggerUIBundle({
                    url: '$specUrl',
                    dom_id: '#swagger-ui',
                    deepLinking: true,
                    presets: [
                        SwaggerUIBundle.presets.apis,
                        SwaggerUIStandalonePreset
                    ],
                    layout: "StandaloneLayout"
                });
            };
        </script>
    </body>
    </html>
""".trimIndent()
