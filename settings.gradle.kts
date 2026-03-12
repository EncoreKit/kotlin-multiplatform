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
include(":shared")
include(":demo")

// Composite build: use local Android SDK source instead of published Maven artifact
includeBuild("../encore-android") {
    dependencySubstitution {
        substitute(module("com.encorekit:encore")).using(project(":encore"))
    }
}
