import org.gradle.kotlin.dsl.invoke

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

allprojects {
    group = "app.simplecloud.plugin"
    version = "1.0-SNAPSHOT"

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
    apply(plugin = "com.gradleup.shadow")

    dependencies {
        testImplementation(rootProject.libs.kotlinTest)
        implementation(rootProject.libs.kotlinJvm)
        implementation(rootProject.libs.bundles.simpleCloudController)
    }

    kotlin {
        jvmToolchain(21)
    }
}

tasks.test {
    useJUnitPlatform()
}
