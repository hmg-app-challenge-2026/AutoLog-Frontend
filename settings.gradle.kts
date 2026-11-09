pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/") // Pleos SDK (vehicle-core 전이 의존)
    }
    versionCatalogs {
        create("libs") { from(files("backend/gradle/libs.versions.toml")) } // Backend와 같은 버전 (스펙 D-04)
        create("fe") { from(files("gradle/fe.versions.toml")) } // 프론트 전용
    }
}
rootProject.name = "AutoLog-Frontend"

includeBuild("backend") {
    dependencySubstitution {
        substitute(module("kr.autolog:vehicle-core")).using(project(":vehicle-core"))
    }
}
include(":vehicle-app")
