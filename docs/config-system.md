# Config System

The config system in `plugin-shared` is built around Configurate YAML serialization.
Most configs should be regular Kotlin classes annotated with `@ConfigSerializable`.

## Main pieces

- `ConfigurationFactory<E>`: loads or creates one config file
- `YamlDirectoryRepository<E, I>`: manages many YAML files in one directory
- `YamlFileConfigurator<E>`: low-level file serializer if you need direct access
- `AbstractMessageConfig`: optional base class for MiniMessage-based message configs

## Single config file

Use `ConfigurationFactory` when you have one config file such as `config.yml`.

```kotlin
import app.simplecloud.plugin.api.shared.config.ConfigurationFactory
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.io.File

@ConfigSerializable
class PluginConfig {
    var prefix: String = "<gray>[<gold>Example<gray>]"
    var enabled: Boolean = true
}

val factory = ConfigurationFactory(
    File(dataDirectory.toFile(), "config.yml"),
    PluginConfig::class.java,
)

val config = factory.loadOrCreate(PluginConfig())

if (config.enabled) {
    println(config.prefix)
}

config.enabled = false
factory.save(config)
```

### Useful methods

- `loadOrCreate(defaultConfig)`: loads the file, or creates it from the default object
- `get()`: returns the cached config after it has been loaded
- `save(entry)`: writes the config back to disk
- `reload()`: reloads the file into the cache

## Directory-based configs

Use `YamlDirectoryRepository` when each entity should live in its own YAML file.
This works well for things like warps, templates, arenas, or message profiles.

```kotlin
import app.simplecloud.plugin.api.shared.config.YamlDirectoryRepository
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.nio.file.Path

@ConfigSerializable
class WarpConfig {
    var name: String = ""
    var world: String = "world"
    var x: Double = 0.0
    var y: Double = 64.0
    var z: Double = 0.0
}

class WarpRepository(
    directory: Path,
) : YamlDirectoryRepository<WarpConfig, String>(
    directory = directory,
    javaClass = WarpConfig::class.java,
) {

    override fun save(entity: WarpConfig) {
        save("${entity.name}.yml", entity)
    }

    override fun find(identifier: String): WarpConfig? {
        return findAll().firstOrNull { it.name.equals(identifier, ignoreCase = true) }
    }
}
```

Usage:

```kotlin
val repository = WarpRepository(dataDirectory.resolve("warps"))

repository.load()

val lobby = WarpConfig().apply {
    name = "lobby"
    world = "world"
    x = 10.0
    y = 65.0
    z = -5.0
}

repository.save(lobby)

val loadedLobby = repository.find("lobby")
val allWarps = repository.findAll()
```

### Repository flow

- implement your own repository class
- extend `YamlDirectoryRepository<E, I>`
- decide how files are named in `save(entity)`
- decide how lookups work in `find(identifier)`
- call `load()` during startup to populate the cache

## Direct YAML access

`YamlFileConfigurator` is the lower-level serializer used by both higher abstractions.
Use it if you want YAML serialization without repository or factory behavior.

```kotlin
import app.simplecloud.plugin.api.shared.config.YamlFileConfigurator

val configurator = YamlFileConfigurator(PluginConfig::class.java)
val file = dataDirectory.resolve("config.yml").toFile()

configurator.save(file, PluginConfig())
val loaded = configurator.load(file)
```

## Message configs

`AbstractMessageConfig` is helpful when your config stores MiniMessage strings and shared variables.

```kotlin
import app.simplecloud.plugin.api.shared.config.AbstractMessageConfig

class MessagesConfig : AbstractMessageConfig() {
    override val variables = mapOf(
        "prefix" to "<gray>[<gold>Example<gray>]"
    )

    val noPermission = "<prefix> <red>You do not have permission."
}
```

Then render a message with:

```kotlin
val component = messagesConfig.msg(messagesConfig.noPermission)
```

## Best practices

- annotate config classes with `@ConfigSerializable`
- prefer mutable `var` fields with default values for YAML-backed classes
- use `ConfigurationFactory` for one file and `YamlDirectoryRepository` for many files
- load repositories during startup so `findAll()` and `find()` work from cache
- keep config classes simple and serialization-friendly
