package dev.emortal.immortal.util

import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.prefix
import dev.emortal.immortal.util.RedisStorage.redisson
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufOutputStream
import net.minestom.server.attribute.Attribute
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.PluginMessagePacket
import net.minestom.server.network.packet.server.play.TeamsPacket
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import java.util.*

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
    additionalHearts = 0f
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
}

fun Player.resetTeam() {
    PermissionUtils.refreshPrefix(this)
    val playerTeam = Manager.team.createBuilder(username)
        .collisionRule(TeamsPacket.CollisionRule.NEVER)
        .prefix("$prefix ".asMini())
        .build()

    team = playerTeam
}

fun Player.sendServer(serverName: String) {
    val out = ByteBufOutputStream(ByteBufAllocator.DEFAULT.buffer())
    out.writeUTF("Connect")
    out.writeUTF(serverName)
    val buffer = out.buffer()
    val bytes = ByteArray(buffer.readableBytes())
    buffer.duplicate().readBytes(bytes)
    out.flush()
    sendPacket(PluginMessagePacket("BungeeCord", bytes))
}

fun UUID.getCachedUsername(): String = redisson.getBucket<String>("${this}username").get()