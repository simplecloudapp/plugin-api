import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("simplecloud.spotless-conventions")
    id("simplecloud.detekt-conventions")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://buf.build/gen/maven")
    maven("https://repo.spectr.is/snapshots")
    maven("https://repo.simplecloud.app/snapshots")
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    testImplementation(kotlin("test"))
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

tasks.processResources {
    val properties = mapOf("version" to project.version.toString())
    inputs.properties(properties)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(properties)
    }
}
