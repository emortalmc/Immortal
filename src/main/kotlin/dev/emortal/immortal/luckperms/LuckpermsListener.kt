package dev.emortal.immortal.luckperms

import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent

class LuckpermsListener(extension: Any, luckperms: LuckPerms) {

    init {

        val eventBus = luckperms.eventBus

        eventBus.subscribe(extension, UserDataRecalculateEvent::class.java) {
            val player = PermissionUtils.userToPlayerMap[it.user] ?: return@subscribe
            PermissionUtils.refreshPrefix(player)
        }

    }

}