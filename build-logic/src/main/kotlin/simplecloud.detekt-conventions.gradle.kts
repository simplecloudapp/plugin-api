import app.simplecloud.gradle.WriteTextFile

plugins {
    id("dev.detekt")
}

val defaultConfigText = checkNotNull(javaClass.getResource("/detekt/detekt.yml")) { "detekt.yml not found" }.readText()
val projectConfig = rootProject.file("config/detekt/detekt.yml")

val extractDetektConfig = tasks.register<WriteTextFile>("extractDetektConfig") {
    description = "Extracts the detekt config"
    text = defaultConfigText
    outputFile = layout.buildDirectory.file("detekt/detekt.yml")
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(extractDetektConfig.flatMap { it.outputFile })
    if (projectConfig.exists()) {
        config.from(projectConfig)
    }
}

pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    tasks.named("check") {
        setDependsOn(dependsOn.filterNot { it is TaskProvider<*> && it.name == "detekt" })
        dependsOn("detektMain", "detektTest")
    }
}
