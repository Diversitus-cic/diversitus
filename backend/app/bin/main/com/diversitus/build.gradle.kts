package com.diversitus

import com.diversitus.data.JobRepository
import com.diversitus.plugins.configureRouting
import com.diversitus.plugins.configureSerialization
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Instantiate dependencies that will be used by the application
    val jobRepository = JobRepository()

    configureSerialization()
    configureRouting(jobRepository)
}