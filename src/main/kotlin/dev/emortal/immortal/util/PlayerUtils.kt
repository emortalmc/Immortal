package dev.emortal.immortal.util

import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.prefix
import dev.emortal.immortal.luckperms.PermissionUtils.rankWeight
import dev.emortal.immortal.util.RedisStorage.redisson
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini

fun Player.reset() {
    inventory.clear()
    isAutoViewable = true
    //entityMeta.setNotifyAboutChanges(false)
    isInvisible = false
    isGlowing = false
    additionalHearts = 0f
    gameMode = GameMode.ADVENTURE
    isAllowFlying = false
    isFlying = false
    food = 20
    level = 0
    isEnableRespawnScreen = false
    vehicle?.removePassenger(this)
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1f
    setCanPickupItem(true)
    setBoundingBox(0.6, 1.8, 0.6)
    closeInventory()
    setNoGravity(false)
    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()
    askSynchronization()
    updateViewableRule()
    updateViewerRule()

    //entityMeta.setNotifyAboutChanges(true)
}

fun Player.resetTeam() {
    PermissionUtils.refreshPrefix(this)
    val playerTeam = Manager.team.createBuilder("${rankWeight?.orElseGet { 0 } ?: 0}${username}")
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .prefix("$prefix ".asMini())
        .build()

    team = playerTeam
    playerTeam.sendUpdatePacket()
}

fun Player.sendServer(gameName: String) {
    redisson?.getTopic("joingame")?.publishAsync("$gameName ${this.uuid}")
}