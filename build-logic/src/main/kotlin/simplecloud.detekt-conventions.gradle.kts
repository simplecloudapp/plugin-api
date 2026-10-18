plugins {
    id("dev.detekt")
}

val defaultConfig = resources.text.fromString(
    checkNotNull(javaClass.getResource("/detekt/detekt.yml")) { "detekt.yml not found" }.readText(),
)
val projectConfig = rootProject.file("config/detekt/detekt.yml")

detekt {
    buildUponDefaultConfig = true
    parallel = true
    config.setFrom(provider { defaultConfig.asFile() })
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
