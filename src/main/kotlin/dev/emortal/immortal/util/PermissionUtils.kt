package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import net.luckperms.api.model.user.User
import net.minestom.server.command.CommandSender
import net.minestom.server.command.ConsoleSender
import net.minestom.server.entity.Player

object PermissionUtils {
    val playerAdapter = ImmortalExtension.luckperms.getPlayerAdapter(Player::class.java)

    val Player.lpUser: User get() = playerAdapter.getUser(this)

    val Player.prefix: String? get() = lpUser.cachedData.metaData.prefix
    val Player.suffix: String? get() = lpUser.cachedData.metaData.suffix

    fun CommandSender.hasLuckPermission(permission: String): Boolean {
        if (this is ConsoleSender) return true
        if (this is Player) {
            return lpUser.cachedData.permissionData.checkPermission(permission).asBoolean()
        }
        return false
    }
}