# Config Migration System

The migration system lets you upgrade old YAML configs to the current structure before they are deserialized.
It is designed to plug directly into `ConfigurationFactory`, `YamlDirectoryRepository`, and `YamlFileConfigurator`.

## Main pieces

- `VersionedConfig`: marks a config as migration-capable
- `ConfigMigrator`: defines the current version and the upgrade steps
- root YAML key `version`: stores the config version on disk

## Defining a versioned config

Your config should implement `VersionedConfig` and expose the current version.

```kotlin
import app.simplecloud.plugin.api.shared.config.VersionedConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class WarpConfig : VersionedConfig {
    override var version: Int = 2

    var name: String = ""
    var world: String = "world"
    var enabled: Boolean = true
}
```

## Creating a migrator

`ConfigMigrator` upgrades a YAML node step by step.
Each migration receives the raw Configurate node, so you can rename fields, move sections, or add defaults.

```kotlin
import app.simplecloud.plugin.api.shared.config.ConfigMigrator

val warpMigrator = ConfigMigrator.builder(currentVersion = 2)
    .migrate(fromVersion = 0, toVersion = 1) { node ->
        val oldWorldName = node.node("location", "worldName").string ?: return@migrate
        node.node("world").set(oldWorldName)
        node.node("location").set(null)
    }
    .migrate(fromVersion = 1, toVersion = 2) { node ->
        if (node.node("enabled").virtual()) {
            node.node("enabled").set(true)
        }
    }
    .build()
```

## Using migrations with a single config

```kotlin
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory

val factory = ConfigurationFactory(
    file = dataDirectory.resolve("config.yml").toFile(),
    javaClass = WarpConfig::class.java,
    configMigrator = warpMigrator,
)

val config = factory.loadOrCreate(WarpConfig())
```

When an older file is loaded:

1. the YAML is parsed
2. the migrator checks the stored `version`
3. all missing migration steps are applied in order
4. the updated YAML is written back to disk
5. the final structure is deserialized into your config class

## Using migrations with a directory repository

```kotlin
import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import java.nio.file.Path

class WarpRepository(
    directory: Path,
) : YamlDirectoryRepository<WarpConfig, String>(
    directory = directory,
    javaClass = WarpConfig::class.java,
    configMigrator = warpMigrator,
) {

    override fun save(entity: WarpConfig) {
        save("${entity.name}.yml", entity)
    }

    override fun find(identifier: String): WarpConfig? {
        return findAll().firstOrNull { it.name == identifier }
    }
}
```

Calling `load()` will migrate each YAML file as it is read.

## Legacy files without a version

Files that do not have a `version` key are treated as version `0` by default.
That makes it easy to introduce migrations for older configs that existed before versioning.

Example legacy file:

```yaml
name: spawn
location:
  worldName: lobby
```

After migration:

```yaml
version: 2
name: spawn
world: lobby
enabled: true
```

## Changing the fallback version

If your oldest supported files should be treated as something other than version `0`, set a fallback version:

```kotlin
val migrator = ConfigMigrator.builder(currentVersion = 3)
    .fallbackVersion(1)
    .migrate(fromVersion = 1, toVersion = 2) { node ->
        node.node("newField").set("default")
    }
    .migrate(fromVersion = 2, toVersion = 3) { node ->
        node.node("anotherField").set(true)
    }
    .build()
```

## Important behavior

- migrations only work for configs that implement `VersionedConfig`
- the migration chain must be complete from the file version to `currentVersion`
- if a file version is newer than the library supports, loading fails
- if a migration step is missing, loading fails
- migrated files are persisted automatically after a successful load

## Best practices

- increase `version` only when the YAML structure changes
- keep each migration small and focused
- prefer additive migrations where possible
- remove old fields explicitly with `node.node("oldField").set(null)` when renaming
- test migrations with real old YAML examples
