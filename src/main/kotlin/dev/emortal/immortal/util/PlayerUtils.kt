package dev.emortal.immortal.util

import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player

fun Player.reset() {
    inventory.clear()
    closeInventory()
    isInvisible = false
    isGlowing = false
    gameMode = GameMode.ADVENTURE
    isAllowFlying = false
    isFlying = false
    food = 20
    level = 0
    setNoGravity(false)
    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()
    askSynchronization()
}