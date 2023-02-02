pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version "7.3.1"
        id("org.jetbrains.kotlin.android") version "1.7.20"
        id("com.android.application") version "7.3.1"
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}
rootProject.name = "helper"
include(":helper", ":helper-kt")
include(":myapplication")
