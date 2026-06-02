import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    // AGP 9 provides built-in Kotlin support; the standalone kotlin-android plugin is no longer
    // applied (it would clash registering the `kotlin` extension). Compiler plugins still apply.
    alias(libs.plugins.kotlin.serialization)
}

// Thin Android layer over arcane-core: secure token storage (Keystore + DataStore) and the
// OIDC browser flow. Mirrors the Swift `ArcaneOIDC` product + `KeychainTokenStore`.
android {
    namespace = "app.getarcane.sdk.android"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    // `api` so consumers transitively see ArcaneClient and the core types.
    api(project(":arcane-core"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto) // for the deprecated EncryptedPrefsTokenStore fallback
    implementation(libs.androidx.browser) // Custom Tabs for the OIDC flow
}
