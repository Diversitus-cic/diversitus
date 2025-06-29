plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

// Add this JVM target configuration
kotlin {
    jvmToolchain(17) // Match your Docker image (amazoncorretto:17)
}

repositories {
    // Explicitly declare Maven Central as the repository for dependencies.
    mavenCentral()
}

dependencies {
    // Ktor - All versions managed by libs.versions.toml
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.lambda)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Logging
    implementation(libs.logback.classic)

    // AWS SDK for Kotlin
    implementation(libs.aws.sdk.dynamodb)

    // Testing
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

application {
    mainClass.set("com.diversitus.ApplicationKt")
}