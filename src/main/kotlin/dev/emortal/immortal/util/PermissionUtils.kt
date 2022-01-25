package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.util.PermissionUtils.suffix
import net.luckperms.api.model.user.User
import net.luckperms.api.platform.PlayerAdapter
import net.luckperms.api.util.Tristate
import net.minestom.server.entity.Player

object PermissionUtils {
    val playerAdapter = ImmortalExtension.luckperms.getPlayerAdapter(Player::class.java)

    val Player.lpUser: User get() = playerAdapter.getUser(this)

    val Player.prefix: String? get() = lpUser.cachedData.metaData.prefix
    val Player.suffix: String? get() = lpUser.cachedData.metaData.suffix

    fun Player.hasPermission(permission: String): Tristate = lpUser.cachedData.permissionData.checkPermission(permission)
}

