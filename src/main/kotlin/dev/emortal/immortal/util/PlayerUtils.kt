package dev.emortal.immortal.util

import dev.emortal.immortal.luckperms.PermissionUtils
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import world.cepi.kstom.Manager

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
//    refreshCommands()
    heal()
    clearEffects()
    stopSpectating()

    Manager.bossBar.getPlayerBossBars(this).forEach {
        Manager.bossBar.removeBossBar(this, it)
    }

    entityMeta.setNotifyAboutChanges(true)

    updateViewableRule { true }
    updateViewerRule { true }
//    askSynchronization()
}

fun Player.resetTeam() {
    PermissionUtils.refreshPrefix(this)

//    val rankWeight = this.rankWeight
//    val actualWeight = if (rankWeight.isPresent) rankWeight.asInt else 9
//
//    val playerTeam = Manager.team.getTeam("${actualWeight}username") ?: Manager.team.createBuilder("${actualWeight}username")
//        .collisionRule(TeamsPacket.CollisionRule.NEVER)
//        .let {
//            if (prefix == null) return@let it
//            it.prefix(prefix!!.asMini())
//        }
//        .build()
//
//    team = playerTeam
}

fun Player.sendServer(gameName: String) {
    JedisStorage.jedis?.publish("joingame", "$gameName ${this.uuid}")
}