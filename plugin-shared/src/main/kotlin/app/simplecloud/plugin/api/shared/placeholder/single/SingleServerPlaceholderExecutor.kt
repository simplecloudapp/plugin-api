package app.simplecloud.plugin.api.shared.placeholder.single

import app.simplecloud.controller.shared.server.Server
import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder

/**
 * @author Niklas Nieberler
 */

class SingleServerPlaceholderExecutor : SinglePlaceholderExecutor<Server> {

    override fun getAsyncPlaceholders() = listOf<AsyncPlaceholder<Server>>(
        AsyncPlaceholder("id") { it.uniqueId },
        AsyncPlaceholder("type") { it.type },
        AsyncPlaceholder("group_name") { it.group },
        AsyncPlaceholder("numerical_id") { it.numericalId },
        AsyncPlaceholder("ip") { it.ip },
        AsyncPlaceholder("port") { it.port },
        AsyncPlaceholder("max_players") { it.maxPlayers },
        AsyncPlaceholder("min_memory") { it.minMemory },
        AsyncPlaceholder("max_memory") { it.maxMemory },
        AsyncPlaceholder("player_count") { it.playerCount }, // TODO: extra option
        AsyncPlaceholder("state") { it.state }
    )

}