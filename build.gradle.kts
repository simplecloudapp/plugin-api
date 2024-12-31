plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.sonatype.central.portal.publisher)
    `maven-publish`
}

val baseVersion = "0.0.1"
val commitHash = System.getenv("COMMIT_HASH")
val snapshotVersion = "${baseVersion}-dev.$commitHash"

allprojects {
    group = "app.simplecloud.plugin"
    version = if (commitHash != null) snapshotVersion else baseVersion

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://libraries.minecraft.net")
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://buf.build/gen/maven")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")

    dependencies {
        compileOnly(rootProject.libs.kotlinJvm)
        compileOnly(rootProject.libs.bundles.simpleCloudController)
        compileOnly(rootProject.libs.bundles.adventure)
    }

    kotlin {
        jvmToolchain(21)
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri("https://repo.simplecloud.app/snapshots/")
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME")?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD")?: (project.findProperty("simplecloudPassword") as? String)
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }

        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    signing {
        if (commitHash != null) {
            return@signing
        }

        sign(publishing.publications)
        useGpgCmd()
    }
}