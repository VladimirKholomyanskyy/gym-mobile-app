plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinAndroidKsp)
    alias(libs.plugins.hiltAndroid)
    id("org.openapi.generator")
}

android {
    namespace = "com.neyra.gymapp"
    compileSdk = 35

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.neyra.gymapp"
        minSdk = 26
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
}
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/contracts/openapi/v1/spec.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().toString())
    packageName.set("com.neyra.gymapp.openapi")
    generateApiTests.set(false)
    generateModelTests.set(false)
    configOptions.put("dateLibrary", "java8")
    configOptions.put("library", "jvm-retrofit2")
    configOptions.put("useCoroutines", "true")
    configOptions.put("serializationLibrary", "gson")
}
kotlin {
    sourceSets {
        main {
            kotlin.srcDir("${layout.buildDirectory.get()}/generated/src/main")
        }
    }
}
dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Choose one of the following:
    // Material Design 3
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    // Android Studio Preview support
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // UI Tests
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.converter.scalars)
    implementation(libs.converter.gson)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m2)
    implementation(libs.vico.compose.m3)
    implementation(libs.vico.views)

    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.aws.auth.cognito)
    implementation(libs.core)

    implementation(libs.timber)

}