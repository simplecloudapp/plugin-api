# Matcher System

The matcher system is a small utility for checking whether names or values match a configured rule.
The main user-facing type is `ServerMatcherConfiguration`.

## Main pieces

- `ServerMatcherConfiguration`: a serializable matcher config
- `OperationType`: the available match operations
- operation matcher implementations: the low-level matching logic

## Basic usage

`ServerMatcherConfiguration` is a YAML-friendly wrapper around an operation, a comparison value, and an optional negation flag.

```kotlin
import app.simplecloud.plugin.api.shared.matcher.OperationType
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration

val matcher = ServerMatcherConfiguration(
    operation = OperationType.STARTS_WITH,
    value = "Lobby-",
    negate = false,
)

val matches = matcher.matches("Lobby-1")
```

## Matching multiple names

```kotlin
val matcher = ServerMatcherConfiguration(
    operation = OperationType.CONTAINS,
    value = "event",
)

val names = listOf("lobby", "event-1", "event-2")

val anyMatches = matcher.anyMatches(names)
val allMatch = matcher.allMatches(names)
```

## Available operations

These operations are available through `OperationType`:

- `REGEX`
- `PATTERN`
- `EQUALS`
- `CONTAINS`
- `STARTS_WITH`
- `ENDS_WITH`
- `GREATER_THAN`

## YAML example

Because `ServerMatcherConfiguration` is `@ConfigSerializable`, it can be used directly inside your config classes.

```kotlin
import app.simplecloud.plugin.api.shared.matcher.ServerMatcherConfiguration
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class ExampleConfig {
    var targetMatcher: ServerMatcherConfiguration = ServerMatcherConfiguration()
}
```

Example YAML:

```yaml
targetMatcher:
  operation: STARTS_WITH
  value: Lobby-
  negate: false
```

## Negation

Use `negate = true` to invert the result.

```kotlin
val matcher = ServerMatcherConfiguration(
    operation = OperationType.ENDS_WITH,
    value = "-dev",
    negate = true,
)

matcher.matches("proxy-1")
```

This returns `true` because `"proxy-1"` does not end with `"-dev"`.

## Choosing an operation

- use `EQUALS` for exact names
- use `STARTS_WITH` or `ENDS_WITH` for prefixes and suffixes
- use `CONTAINS` for simple substring checks
- use `REGEX` when you need explicit regular expressions
- use `PATTERN` only if you specifically want the alternate Java `Pattern`-based matcher behavior in this library

## Important limitation

`ServerMatcherConfiguration` currently stores its comparison `value` as a `String`.
That means it is mainly intended for string-based matching.

`GREATER_THAN` exists in the lower-level `OperationType` API for integer comparisons, but it is not a natural fit for `ServerMatcherConfiguration` because that wrapper does not store numeric values.

## Best practices

- use `ServerMatcherConfiguration` inside other config classes when you want user-configurable filters
- prefer simple operations like `EQUALS` or `STARTS_WITH` unless you really need regex
- document the expected input format when exposing `REGEX` or `PATTERN` to end users
