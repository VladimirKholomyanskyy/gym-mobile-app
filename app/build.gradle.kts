plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("org.openapi.generator")
}

android {
    namespace = "com.neyra.gymmobileapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.neyra.gymmobileapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11" // ✅ Updated version
    }

    openApiGenerate {
        generatorName.set("kotlin")
        inputSpec.set("$rootDir/openapi/openapi-spec.yaml")
        outputDir.set(layout.buildDirectory.dir("generated").get().toString())
        apiPackage.set("com.neyra.gymmobileapp.openapi.api")
        invokerPackage.set("com.neyra.gymmobileapp.openapi.invoker")
        modelPackage.set("com.neyra.gymmobileapp.openapi.model")
        configOptions.put("dateLibrary", "java8")
        configOptions.put("library","jvm-retrofit2")
        configOptions.put("useCoroutines","true")
        configOptions.put("packageName","com.neyra.gymmobileapp.openapi.client")
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.ui.tooling.preview.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coroutines for async tasks
    implementation(libs.kotlinx.coroutines.android)

    // Dependency Injection (optional but recommended)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Jetpack Compose UI (if using Compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)

}