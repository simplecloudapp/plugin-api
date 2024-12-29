package app.simplecloud.plugin.api.shared.placeholder.single

import app.simplecloud.controller.shared.group.Group
import app.simplecloud.plugin.api.shared.placeholder.async.AsyncPlaceholder

/**
 * @author Niklas Nieberler
 */

class SingleGroupPlaceholderExecutor : SinglePlaceholderExecutor<Group> {

    override fun getAsyncPlaceholders() = listOf<AsyncPlaceholder<Group>>(
        AsyncPlaceholder("name") { it.name },
        AsyncPlaceholder("type") { it.type },
        AsyncPlaceholder("max_players") { it.maxPlayers },
        AsyncPlaceholder("min_memory") { it.minMemory },
        AsyncPlaceholder("max_memory") { it.maxMemory },
        AsyncPlaceholder("start_port") { it.startPort },
        AsyncPlaceholder("min_online_count") { it.minOnlineCount },
        AsyncPlaceholder("max_online_count") { it.maxOnlineCount },
    )

}