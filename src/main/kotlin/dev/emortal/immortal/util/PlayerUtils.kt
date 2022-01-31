package dev.emortal.immortal.util

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import org.slf4j.LoggerFactory

fun Player.reset() {
    inventory.clear()
    isAutoViewable = true
    isInvisible = false
    isGlowing = false
    gameMode = GameMode.ADVENTURE
    isAllowFlying = false
    isFlying = false
    food = 20
    level = 0
    isEnableRespawnScreen = false
    setCanPickupItem(true)
    closeInventory()
    setNoGravity(false)
    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()
    askSynchronization()
}