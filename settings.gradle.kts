// Configures the repositories where Gradle will look for project plugins.
pluginManagement {
    repositories {
        google {
            // Optimizes build speed by routing specific Android and Google plugin requests exclusively to the Google repository.
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Adds JitPack repository to resolve plugins hosted on GitHub.
        maven { url = uri("https://jitpack.io") }
    }
}

// Centralizes dependency resolution to ensure all project modules use the same repositories.
dependencyResolutionManagement {
    // Forces all project modules to use these centralized repositories, failing the build if a module tries to define its own.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Adds JitPack repository to resolve library dependencies (e.g., third-party libraries like MPAndroidChart).
        maven { url = uri("https://jitpack.io") }
    }
}

// Defines the root name of the project.
rootProject.name = "MapMyTasks"
// Includes the main application module in the build process.
include(":app")