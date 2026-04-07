@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    jvm()
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            // No external dependencies — domain is framework-agnostic by design
        }
    }
}

android {
    namespace = "net.kustax.opendelivery.domain"
    compileSdk = 36
    defaultConfig {
        minSdk = 30
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Tests are intentionally excluded from this project
    sourceSets {
        getByName("test").java.setSrcDirs(emptyList<Any>())
        getByName("androidTest").java.setSrcDirs(emptyList<Any>())
    }
}
