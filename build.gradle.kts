// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("org.openapi.generator") version "7.10.0"
    alias(libs.plugins.hiltAndroid) apply false
    alias(libs.plugins.kotlinAndroidKsp) apply false
}

tasks.register("updateOpenAPI") {
    group = "openapi"
    description = "Update OpenAPI spec and regenerate client"

    doFirst {
        exec {
            commandLine("git", "submodule", "update", "--remote")
        }
    }

//    finalizedBy("openApiGenerate")
}