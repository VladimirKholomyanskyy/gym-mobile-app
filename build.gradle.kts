// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    id("com.google.devtools.ksp") version "2.1.10-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.openapi.generator") version "7.10.0"
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