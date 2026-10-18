plugins {
    id("maven-publish")
    id("simplecloud.base-conventions") apply false
}

val baseVersion = "1.0.0"
val isRelease = providers.gradleProperty("release").isPresent
val commitHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim()

allprojects {
    group = "app.simplecloud.plugin"
    version = if (isRelease) baseVersion else "$baseVersion-dev.$commitHash"
}

subprojects {
    apply {
        plugin("maven-publish")
        plugin("simplecloud.base-conventions")
    }

    dependencies {
        "compileOnly"(rootProject.libs.kotlin.jvm)
        "compileOnly"(rootProject.libs.kotlin.coroutines)
        "compileOnly"(rootProject.libs.simplecloud.api)
        "compileOnly"(rootProject.libs.bundles.cloud)
        "compileOnly"(rootProject.libs.bundles.adventure)
        "compileOnly"(rootProject.libs.bundles.configurate)
        "compileOnly"(rootProject.libs.slf4j.api)
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }

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

}
