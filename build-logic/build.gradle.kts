plugins {
    `kotlin-dsl`
    `maven-publish`
}

val baseVersion = "1.0.0"
val isRelease = providers.gradleProperty("release").isPresent
val commitHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

group = "app.simplecloud.gradle"
version = if (isRelease) baseVersion else "$baseVersion-dev.$commitHash"

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.shadow.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
}

publishing {
    repositories {
        maven {
            name = "simplecloud"
            url = uri("https://repo.simplecloud.app/snapshots")
            credentials {
                username = System.getenv("SIMPLECLOUD_USERNAME")
                password = System.getenv("SIMPLECLOUD_PASSWORD")
            }
        }
    }
}
