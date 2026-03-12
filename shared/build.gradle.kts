import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    // Pin language/API version so consumers on Kotlin 1.9+ don't need to upgrade their compiler.
    // We compile with 2.0.x but emit 1.9-compatible metadata and bytecode.
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_1_9)
        apiVersion.set(KotlinVersion.KOTLIN_1_9)
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
    }

    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_17)
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Encore KMP SDK"
        homepage = "https://encorekit.com"
        version = "0.1.0"
        ios.deploymentTarget = "16.0"

        framework {
            baseName = "EncoreKMP"
            isStatic = true
        }

        // Native iOS SDK — linked as transitive dependency of EncoreKMPBridge.
        // linkOnly skips cinterop (Kotlin code imports EncoreKMPBridge, not EncoreKit directly).
        pod("EncoreKit") {
            version = "~> 1.4"
            linkOnly = true
        }

        // Local bridge pod — @objc wrapper for Swift-only EncoreKit APIs
        pod("EncoreKMPBridge") {
            source = path(project.file("../iosHelper"))
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.encore.android)
            implementation(libs.kotlinx.coroutines.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.encorekit.kmp"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
