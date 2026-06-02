// Root build script. Plugins are declared here (apply false) and applied per-module.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
}

// group/version on all projects so a consuming composite build (the Android app's
// `includeBuild("../libarcane-kotlin")`) can substitute `app.getarcane:arcane-core` /
// `app.getarcane:arcane-android` with these local modules.
allprojects {
    group = "app.getarcane"
    version = "0.1.0-SNAPSHOT"
}
