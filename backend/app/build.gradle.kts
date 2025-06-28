plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
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

// This task creates a single "fat" JAR file containing all dependencies.
tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output) { include("**/.*") }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}