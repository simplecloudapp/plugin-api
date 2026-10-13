plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    "plugin-bungeecord",
    "plugin-paper",
    "plugin-shared",
    "plugin-velocity"
)

rootProject.name = "plugin-api"