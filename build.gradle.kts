import org.gradle.kotlin.dsl.invoke

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

    dependencies {
        compileOnly(rootProject.libs.kotlinJvm)
        compileOnly(rootProject.libs.bundles.simpleCloudController)
        compileOnly(rootProject.libs.bundles.adventure)
    }

    kotlin {
        jvmToolchain(21)
    }
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

centralPortal {
    name = project.name

    username = project.findProperty("sonatypeUsername") as? String
    password = project.findProperty("sonatypePassword") as? String

    pom {
        name.set("SimpleCloud Plugin API")
        description.set("Commonly used classes among plugins")
        url.set("https://github.com/theSimpleCloud/plugin-api")
        developers {
            developer {
                id.set("MrManHD")
            }
        }
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/theSimpleCloud/plugin-api.git")
            connection.set("git:git@github.com:theSimpleCloud/plugin-api.git")
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