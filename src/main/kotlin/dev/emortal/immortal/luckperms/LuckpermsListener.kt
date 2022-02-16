package dev.emortal.immortal.luckperms

import dev.emortal.immortal.util.PermissionUtils
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import net.minestom.server.extensions.Extension

class LuckpermsListener(extension: Extension, luckperms: LuckPerms) {

    init {

        val eventBus = luckperms.eventBus

        eventBus.subscribe(extension, UserDataRecalculateEvent::class.java) {
            val player = PermissionUtils.userToPlayerMap[it.user] ?: return@subscribe
            PermissionUtils.refreshPrefix(player)
        }

    }

}