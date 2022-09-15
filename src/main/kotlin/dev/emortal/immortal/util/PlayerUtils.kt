package dev.emortal.immortal.util

import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.prefix
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini

fun Player.reset() {
    entityMeta.setNotifyAboutChanges(false)

    inventory.clear()
    isAutoViewable = true
    isInvisible = false
    isGlowing = false
    isSneaking = false
    isAllowFlying = false
    isFlying = false
    additionalHearts = 0f
    gameMode = GameMode.ADVENTURE
    food = 20
    level = 0
    vehicle?.removePassenger(this)
    arrowCount = 0
    setFireForDuration(0)
    getAttribute(Attribute.MOVEMENT_SPEED).baseValue = 0.1f
    setCanPickupItem(true)
    if (openInventory != null) closeInventory()
    setNoGravity(false)
    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()

    Manager.bossBar.getPlayerBossBars(this).forEach {
        Manager.bossBar.removeBossBar(this, it)
    }

    entityMeta.setNotifyAboutChanges(true)

    updateViewableRule()
    updateViewerRule()
    askSynchronization()
}

fun Player.resetTeam() {
    PermissionUtils.refreshPrefix(this)

    val playerTeam = Manager.team.getTeam("$${username}") ?: Manager.team.createBuilder("${username}")
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .let {
            if (prefix == null) return@let it
            it.prefix("$prefix ".asMini())
        }
        .build()

    team = playerTeam
}

fun Player.sendServer(gameName: String) {
    RedisStorage.joinGameTopic?.publish("$gameName ${this.uuid}")
}