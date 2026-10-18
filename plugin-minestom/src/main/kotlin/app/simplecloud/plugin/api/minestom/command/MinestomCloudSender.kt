package app.simplecloud.plugin.api.minestom.command

import app.simplecloud.plugin.api.minestom.permission.MinestomPermissions
import app.simplecloud.plugin.api.shared.command.CloudSender
import net.kyori.adventure.text.Component
import net.minestom.server.command.CommandSender
import net.minestom.server.entity.Player
import java.util.UUID

class MinestomCloudSender(private val commandSender: CommandSender, private val permissions: MinestomPermissions) :
    CloudSender {

    fun getCommandSender(): CommandSender = commandSender

    override fun getUniqueId(): UUID? = (commandSender as? Player)?.uuid

    override fun hasPermission(permission: String): Boolean = permissions.hasPermission(commandSender, permission)

    override fun sendMessage(message: Component) {
        commandSender.sendMessage(message)
    }
}
