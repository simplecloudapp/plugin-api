package app.simplecloud.plugin.api.shared.placeholder.argument.group

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.group.Group
import app.simplecloud.plugin.api.shared.placeholder.argument.ArgumentsResolver
import build.buf.gen.simplecloud.controller.v1.ServerState
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.sumOf

/**
 * @author Niklas Nieberler
 */

class PlayerCountArgumentsResolver(
    private val controllerApi: ControllerApi.Coroutine,
    private val group: Group,
) : ArgumentsResolver {

    override fun getKey() = "player_count"

    override suspend fun resolve(arguments: ArgumentQueue): Tag? {
        val text = arguments.popOr("all").value()
        val serverState = ServerState.entries.firstOrNull { it.name.equals(text, true) }
        return Tag.preProcessParsed(findPlayerCount(this.group, serverState).toString())
    }

    private suspend fun findPlayerCount(group: Group, state: ServerState?): Long {
        return this.controllerApi.getServers().getServersByGroup(group)
            .filter { it.state == state }
            .sumOf { it.playerCount }
    }

}