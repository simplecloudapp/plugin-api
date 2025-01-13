package app.simplecloud.plugin.api.shared.placeholder.single

import app.simplecloud.controller.api.ControllerApi
import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder
import app.simplecloud.plugin.api.shared.pretty.StringPrettifier

/**
 * @author Niklas Nieberler
 */

class SingleServerPlaceholderExecutor : SinglePlaceholderExecutor<Server> {

    override fun getAsyncPlaceholders(controllerApi: ControllerApi.Coroutine) = listOf<AsyncPlaceholder<Server>>(
        AsyncPlaceholder("id") { it.uniqueId },
        AsyncPlaceholder("numerical_id") { it.numericalId },
        AsyncPlaceholder("group_name") { it.group },
        AsyncPlaceholder("group_pretty_name") {
            it.properties["pretty-name"] ?: StringPrettifier.prettify(it.group)
        },
        AsyncPlaceholder("type") { it.type },
        AsyncPlaceholder("state") { it.state },
        AsyncPlaceholder("ip") { it.ip },
        AsyncPlaceholder("port") { it.port },
        AsyncPlaceholder("online_players") { it.playerCount },
        AsyncPlaceholder("max_players") { it.maxPlayers },
        AsyncPlaceholder("min_memory") { it.minMemory },
        AsyncPlaceholder("max_memory") { it.maxMemory },
        AsyncPlaceholder("motd") { it.properties["motd"] ?: "A Minecraft server" }
    )

}