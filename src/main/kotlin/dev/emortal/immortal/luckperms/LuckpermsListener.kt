package dev.emortal.immortal.luckperms

import net.luckperms.api.LuckPerms
import net.luckperms.api.event.user.UserDataRecalculateEvent
import world.cepi.kstom.Manager

internal class LuckpermsListener(extension: Any, luckperms: LuckPerms) {

    init {

        val eventBus = luckperms.eventBus

        eventBus.subscribe(extension, UserDataRecalculateEvent::class.java) {
            val player = Manager.connection.getPlayer(it.user.uniqueId) ?: return@subscribe
            PermissionUtils.refreshPrefix(player)
        }

    }

}