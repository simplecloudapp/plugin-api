package app.simplecloud.plugin.api.bungeecord.command

import net.kyori.adventure.platform.bungeecord.BungeeAudiences
import net.md_5.bungee.api.CommandSender
import org.incendo.cloud.SenderMapper

class BungeeCloudSenderMapper(
    private val audiences: BungeeAudiences
) : SenderMapper<CommandSender, BungeeCloudSender> {

    override fun map(base: CommandSender): BungeeCloudSender {
        return BungeeCloudSender(base, audiences)
    }

    override fun reverse(mapped: BungeeCloudSender): CommandSender {
        return mapped.getCommandSender()
    }

}
