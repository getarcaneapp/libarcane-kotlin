import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

// Pure Kotlin/JVM library. No Android dependencies — runs and unit-tests on any JVM
// (Ktor MockEngine).
kotlin {
    explicitApi()
    compilerOptions {
        // Target Java 17 bytecode (class file v61) — safe for any Android consumer — while
        // compiling with the JDK 21 that runs Gradle. Avoids JVM toolchain auto-provisioning.
        jvmTarget = JvmTarget.JVM_17
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    // Exposed to consumers (Flow, Ktor types, serialization, Instant appear in the public API).
    api(libs.ktor.client.core)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.datetime)

    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.websockets)
    // `api`: LogLevel appears in ArcaneConfiguration's public API.
    api(libs.ktor.client.logging)
    // Default engine bundled for convenience; consumers may exclude and supply their own.
    implementation(libs.ktor.client.cio)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
