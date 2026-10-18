plugins {
    id("dev.detekt")
}

val defaultConfigText = checkNotNull(javaClass.getResource("/detekt/detekt.yml")) { "detekt.yml not found" }.readText()
val projectConfig = rootProject.file("config/detekt/detekt.yml")

val extractDetektConfig = tasks.register("extractDetektConfig") {
    val output = layout.buildDirectory.file("detekt/detekt.yml")
    inputs.property("config", defaultConfigText)
    outputs.file(output)
    doLast { output.get().asFile.writeText(defaultConfigText) }
}

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(extractDetektConfig)
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
