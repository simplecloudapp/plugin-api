package app.simplecloud.plugin.api.shared.config

/**
 * Marks a config as versioned, so it can be used with a [ConfigMigrator].
 */
interface VersionedConfig {
    val version: Int
}
