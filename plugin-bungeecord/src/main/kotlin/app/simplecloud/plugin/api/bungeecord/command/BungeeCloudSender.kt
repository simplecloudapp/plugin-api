package app.simplecloud.plugin.api.bungeecord.command

import app.simplecloud.plugin.api.shared.command.CloudSender
import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.kyori.adventure.text.Component
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.connection.ProxiedPlayer
import java.util.UUID

class BungeeCloudSender(
    private val commandSender: CommandSender,
    private val audiences: BungeeAudiences
) : CloudSender {

    fun getCommandSender(): CommandSender {
        return commandSender
    }

    override fun getUniqueId(): UUID {
        return (commandSender as? ProxiedPlayer)?.uniqueId ?: UUID(0L, 0L)
    }

    override fun hasPermission(permission: String): Boolean {
        return commandSender.hasPermission(permission)
    }

    override fun sendMessage(message: Component) {
        audiences.sender(commandSender).sendMessage(message)
    }

}
