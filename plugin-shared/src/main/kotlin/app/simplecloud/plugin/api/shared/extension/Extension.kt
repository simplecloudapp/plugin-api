package app.simplecloud.plugin.api.shared.extension

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.PlaceholderProvider
import app.simplecloud.plugin.api.shared.placeholder.argument.ArgumentsResolver
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

/**
 * @author Niklas Nieberler
 */

val miniMessage = MiniMessage.miniMessage()

fun text(message: String, vararg tagResolver: TagResolver): Component {
    return miniMessage.deserialize(message, *tagResolver)
}

suspend fun String.appendToComponent(
    server: Server,
    prefix: String? = null,
    vararg argumentsResolver: ArgumentsResolver,
): Component {
    return PlaceholderProvider.serverPlaceholderProvider.append(server, this, prefix, *argumentsResolver)
}

suspend fun String.appendToComponent(
    group: Group,
    prefix: String? = null,
    vararg argumentsResolver: ArgumentsResolver
): Component {
    return PlaceholderProvider.groupPlaceholderProvider.append(group, this, prefix, *argumentsResolver)
}