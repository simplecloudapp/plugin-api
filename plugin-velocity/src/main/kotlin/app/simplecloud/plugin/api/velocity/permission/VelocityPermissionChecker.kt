package app.simplecloud.plugin.api.velocity.permission

import app.simplecloud.plugin.api.shared.permission.PermissionChecker
import com.velocitypowered.api.proxy.Player

class VelocityPermissionChecker : PermissionChecker<Player> {

    override fun checkPermission(player: Player, name: String): Boolean = player.hasPermission(name)
}
