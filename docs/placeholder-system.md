# Placeholder System

The placeholder system builds MiniMessage `TagResolver`s for SimpleCloud objects such as servers, groups, and persistent servers.
It is designed to turn strings like `<name>` or `<property:motd>` into rendered Adventure components.

## Main pieces

- `PlaceholderProvider`: entry point for the built-in providers
- `ServerPlaceholderProvider`
- `GroupPlaceholderProvider`
- `PersistentServerPlaceholderProvider`
- `ArgumentsResolver`: dynamic placeholders with arguments such as `<property:motd>`
- `String.appendToComponent(...)`: convenience extensions for rendering messages

## Quick start

If you already have a `Server`, `Group`, or `PersistentServer`, the easiest API is the extension function on `String`.

```kotlin
import app.simplecloud.plugin.api.shared.extension.appendToComponent

val component = "<group_name> is online on <ip>:<port>".appendToComponent(server)
```

This returns an Adventure `Component`.

## Built-in entry points

The shared object exposes three built-in providers:

```kotlin
import app.simplecloud.plugin.api.shared.placeholder.PlaceholderProvider

val serverProvider = PlaceholderProvider.serverPlaceholderProvider
val groupProvider = PlaceholderProvider.groupPlaceholderProvider
val persistentServerProvider = PlaceholderProvider.persistentServerPlaceholderProvider
```

You can use the providers directly if you want a `TagResolver` or if you want to append placeholders to a string manually.

## Server placeholders

Server placeholders are backed by `SingleServerPlaceholderExecutor`.
Available keys include:

- `<id>`
- `<numerical_id>`
- `<group_name>`
- `<group_pretty_name>`
- `<type>`
- `<state>`
- `<ip>`
- `<port>`
- `<online_players>`
- `<max_players>`
- `<min_memory>`
- `<max_memory>`
- `<motd>`

Example:

```kotlin
val component = "<group_pretty_name> <gray>(<online_players>/<max_players>)".appendToComponent(server)
```

## Group placeholders

Group placeholders are backed by `SingleGroupPlaceholderExecutor`.
Available keys include:

- `<name>`
- `<type>`
- `<max_players>`
- `<min_memory>`
- `<max_memory>`
- `<online_players>`

Example:

```kotlin
val component = "<name> has <online_players> players online".appendToComponent(group)
```

## Persistent server placeholders

Persistent server placeholders are backed by `SinglePersistentServerPlaceholderExecutor`.
Available keys include:

- `<id>`
- `<name>`
- `<pretty_name>`
- `<type>`
- `<online_players>`
- `<max_players>`
- `<min_memory>`
- `<max_memory>`
- `<motd>`

Example:

```kotlin
val component = "<pretty_name> is running on type <type>".appendToComponent(persistentServer)
```

## Argument-based placeholders

Some placeholders accept arguments.
These are powered by `ArgumentsResolver`.

Built-in resolvers include:

- `PropertiesArgumentsResolver`: `<property:key[:default]>`
- `EnvironmentArgumentsResolver`: `<env:NAME[:default]>`

Examples:

```kotlin
val component = "<property:motd:Fallback MOTD>".appendToComponent(server)
val component2 = "<env:HOSTNAME:unknown>".appendToComponent(server)
```

For groups, additional resolvers are registered automatically:

- `<player_count>` or `<player_count:ONLINE>`
- `<server_count>` or `<server_count:ONLINE>`

## Prefixing placeholders

You can prefix all generated keys to avoid collisions when combining multiple resolver sets.

```kotlin
val component = PlaceholderProvider.serverPlaceholderProvider.append(
    server,
    "<source_name> -> <source_ip>:<source_port>",
    prefix = "source",
)
```

With `prefix = "source"`, `<name>` becomes `<source_name>` and `<ip>` becomes `<source_ip>`.

## Working with multiple values

The providers can also build a resolver from multiple values.
That is mostly useful when you want argument resolvers aggregated from several objects.

```kotlin
val resolver = PlaceholderProvider.groupPlaceholderProvider.getTagResolver(listOf(groupA, groupB))
```

You can then pass that resolver into your own MiniMessage rendering flow.

## Direct provider usage

```kotlin
val resolver = PlaceholderProvider.serverPlaceholderProvider.getTagResolver(listOf(server))
val component = PlaceholderProvider.serverPlaceholderProvider.append(
    server,
    "<group_name> <gray>- <online_players> players"
)
```

## Custom argument resolvers

You can contribute additional resolvers when rendering:

```kotlin
import app.simplecloud.plugin.api.shared.placeholder.argument.ArgumentsResolver
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue

class RankResolver : ArgumentsResolver {
    override fun getKey() = "rank"

    override suspend fun resolve(arguments: ArgumentQueue): Tag? {
        val playerName = arguments.popOr("player expected").value()
        return Tag.preProcessParsed(fetchRank(playerName))
    }
}

val component = "<group_name> <gray>- rank: <rank:Steve>".appendToComponent(
    server,
    RankResolver(),
)
```

## Best practices

- use the `String.appendToComponent(...)` extensions for the most concise API
- use prefixes when combining resolvers from different domains
- keep custom argument resolvers fast and predictable
- supply sensible defaults for property- and environment-based placeholders
