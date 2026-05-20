pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lifttr"
include(":app")
include(":benchmark")

// Core modules
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:ml")

// Feature modules
include(":feature:auth")
include(":feature:workout")
include(":feature:summary")
include(":feature:history")
include(":feature:analytics")
include(":feature:profile")
