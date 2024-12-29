import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.shadow)
}

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

dependencies {
    testImplementation(rootProject.libs.kotlinTest)
    implementation(rootProject.libs.kotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    expand("version" to project.version,
        "name" to project.name)
}

tasks.shadowJar {
    relocate("io.grpc", "app.simplecloud.relocate.grpc")
    relocate("app.simplecloud.controller", "app.simplecloud.relocate.controller")
    relocate("app.simplecloud.pubsub", "app.simplecloud.relocate.pubsub")
    relocate("app.simplecloud.droplet", "app.simplecloud.relocate.droplet")
    relocate("build.buf.gen", "app.simplecloud.relocate.buf")
    relocate("com.google.protobuf", "app.simplecloud.relocate.protobuf")
}