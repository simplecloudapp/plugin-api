package app.simplecloud.plugin.api.velocity.command

import app.simplecloud.plugin.api.shared.command.CloudSender
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.util.UUID
import net.kyori.adventure.text.Component

class VelocityCloudSender(private val commandSource: CommandSource) : CloudSender {

    fun getCommandSource(): CommandSource {
        return commandSource
    }

    override fun getUniqueId(): UUID? {
        return (commandSource as? Player)?.uniqueId
    }

    override fun hasPermission(permission: String): Boolean {
        return commandSource.hasPermission(permission)
    }

    override fun sendMessage(message: Component) {
        commandSource.sendMessage(message)
    }
}
