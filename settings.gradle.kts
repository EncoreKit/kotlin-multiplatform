pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "encore-kmp"
include(":encore-kmp")
project(":encore-kmp").projectDir = file("shared")
include(":demo")
