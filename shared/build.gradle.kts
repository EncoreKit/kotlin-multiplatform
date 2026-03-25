import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
}

group = "com.encorekit"
version = findProperty("VERSION_NAME") as String

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
        publishLibraryVariants("release")
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
        ios.deploymentTarget = "15.0"

        framework {
            baseName = "EncoreKMP"
            isStatic = true
        }

        // Native iOS SDK — linked as transitive dependency of EncoreKMPBridge.
        // linkOnly skips cinterop (Kotlin code imports EncoreKMPBridge, not EncoreKit directly).
        pod("EncoreKit") {
            version = libs.versions.encore.ios.get()
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

// -- Maven Central Publishing --

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Encore KMP SDK")
            description.set("Kotlin Multiplatform SDK for Encore — thin bridge to native iOS and Android SDKs")
            url.set("https://encorekit.com")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("encorekit")
                    name.set("EncoreKit")
                    email.set("dev@encorekit.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/EncoreKit/kotlin-multiplatform.git")
                developerConnection.set("scm:git:ssh://github.com/EncoreKit/kotlin-multiplatform.git")
                url.set("https://github.com/EncoreKit/kotlin-multiplatform")
            }
        }
    }
}

signing {
    val envKey: String = System.getenv("GPG_SIGNING_KEY").orEmpty()
    val envPassword: String = System.getenv("GPG_SIGNING_PASSWORD").orEmpty()
    val propKeyId: String? = findProperty("signing.keyId")?.toString()
    val propKey: String? = findProperty("signing.key")?.toString()
    val propPassword: String? = findProperty("signing.password")?.toString()

    when {
        envKey.isNotBlank() -> useInMemoryPgpKeys(envKey, envPassword)
        propKey != null -> useInMemoryPgpKeys(propKeyId, propKey, propPassword ?: "")
        propKeyId != null -> useGpgCmd()
    }

    isRequired = envKey.isNotBlank() || propKey != null || propKeyId != null
    sign(publishing.publications)
}
