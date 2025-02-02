import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.sonatype.central.portal.publisher)
    `maven-publish`
}

allprojects {
    group = "app.simplecloud.plugin"
    version = determineVersion()

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://libraries.minecraft.net")
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://buf.build/gen/maven")
        maven("https://repo.papermc.io/repository/maven-public")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "net.thebugmc.gradle.sonatype-central-portal-publisher")

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        compileOnly(rootProject.libs.kotlin.jvm)
        compileOnly(rootProject.libs.bundles.simpleCloudController)
        compileOnly(rootProject.libs.bundles.adventure)
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    kotlin {
        jvmToolchain(21)

        compilerOptions {
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri(determineRepositoryUrl())
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME")
                        ?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD")
                        ?: (project.findProperty("simplecloudPassword") as? String)
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
        val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
        if (releaseType != "release") {
            return@signing
        }

        if (hasProperty("signingPassphrase")) {
            val signingKey: String? by project
            val signingPassphrase: String? by project
            useInMemoryPgpKeys(signingKey, signingPassphrase)
        } else {
            useGpgCmd()
        }

        sign(publishing.publications)
    }

    tasks.jar {
        archiveVersion.set("")
    }
}

fun determineVersion(): String {
    val baseVersion = project.findProperty("baseVersion")?.toString() ?: "0.0.0"
    val releaseType = project.findProperty("releaseType")?.toString() ?: "snapshot"
    val commitHash = System.getenv("COMMIT_HASH") ?: "local"

    return when (releaseType) {
        "release" -> baseVersion
        "rc" -> "$baseVersion-rc.$commitHash"
        "snapshot" -> "$baseVersion-SNAPSHOT.$commitHash"
        else -> "$baseVersion-SNAPSHOT.local"
    }
}

fun determineRepositoryUrl(): String {
    val baseUrl = "https://repo.simplecloud.app/"
    return when (project.findProperty("releaseType")?.toString() ?: "snapshot") {
        "release" -> "$baseUrl/releases"
        "rc" -> "$baseUrl/rc"
        else -> "$baseUrl/snapshots"
    }
}