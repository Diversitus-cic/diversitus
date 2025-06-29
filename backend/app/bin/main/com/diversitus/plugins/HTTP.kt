package com.diversitus.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        // For production, it's best practice to be specific about allowed origins.
        // Replace with your actual frontend domain if different.
        allowHost("app.loveable.dev", schemes = listOf("https"))
        // You can add localhost for local development if needed
        allowHost("localhost:3000")
    }
}