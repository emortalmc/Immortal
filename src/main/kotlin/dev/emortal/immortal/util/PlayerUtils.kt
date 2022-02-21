package dev.emortal.immortal.util

import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.prefix
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.TeamBuilder
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini

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
    vehicle?.removePassenger(this)
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1f
    setCanPickupItem(true)
    closeInventory()
    setNoGravity(false)
    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()
    askSynchronization()
}

fun Player.resetTeam() {
    PermissionUtils.refreshPrefix(this)
    val playerTeam = TeamBuilder(username, Manager.team)
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .prefix("$prefix ".asMini())
        .build()

    team = playerTeam
}