import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("maven-publish")
    alias(libs.plugins.kotlin)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://buf.build/gen/maven")
        maven("https://repo.simplecloud.app/snapshots")
        maven("https://repo.papermc.io/repository/maven-public")
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }

    dependencies {
        testImplementation(rootProject.libs.kotlin.test)
        compileOnly(rootProject.libs.kotlin.jvm)
        compileOnly(rootProject.libs.kotlin.coroutines)
        compileOnly(rootProject.libs.simplecloud.api)
        compileOnly(rootProject.libs.bundles.adventure)
        compileOnly(rootProject.libs.bundles.configurate)
        compileOnly(rootProject.libs.slf4j.api)
    }

    kotlin {
        jvmToolchain(25)
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            languageVersion = KotlinVersion.KOTLIN_2_4
            apiVersion = KotlinVersion.KOTLIN_2_4
        }
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.test {
        useJUnitPlatform()
    }

    publishing {
        repositories {
            maven {
                name = "simplecloud"
                url = uri("https://repo.simplecloud.app/snapshots/")
                credentials {
                    username = System.getenv("SIMPLECLOUD_USERNAME") ?: (project.findProperty("simplecloudUsername") as? String)
                    password = System.getenv("SIMPLECLOUD_PASSWORD") ?: (project.findProperty("simplecloudPassword") as? String)
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

}