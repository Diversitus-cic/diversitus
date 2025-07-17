package com.diversitus.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import java.net.URI

fun Application.configureHTTP() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.ContentType)

        // --- Production & Staging Domains ---
        // Allow your primary domain and its www subdomain.
        allowHost("diversitus.uk", schemes = listOf("http", "https"))
        allowHost("www.diversitus.uk", schemes = listOf("http", "https"))

        // --- Development Domains ---
        // Use a predicate to dynamically allow development domains
        allowOrigins { origin ->
            try {
                val uri = URI.create(origin)
                when {
                    // Allow any subdomain of lovableproject.com over HTTPS
                    uri.host != null && uri.host.endsWith("lovableproject.com") && uri.scheme == "https" -> true
                    // Allow any subdomain of lovable.dev over HTTPS
                    uri.host != null && uri.host.endsWith("lovable.dev") && uri.scheme == "https" -> true
                    // Allow any subdomain of lovable.app over HTTPS (for Loveable preview environments)
                    uri.host != null && uri.host.endsWith("lovable.app") && uri.scheme == "https" -> true
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }

        // --- Local Development ---
        allowHost("localhost:3000", schemes = listOf("http"))
        allowHost("localhost:5173", schemes = listOf("http")) // Common Vite/SvelteKit dev port
    }
}