plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    group = "com.diversitus"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}