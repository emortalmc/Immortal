package dev.emortal.immortal.util

import net.kyori.adventure.sound.Sound
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.other.FireworkRocketMeta
import net.minestom.server.instance.Instance
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.firework.FireworkEffect
import net.minestom.server.item.metadata.FireworkMeta
import net.minestom.server.network.packet.server.play.EntityStatusPacket
import net.minestom.server.sound.SoundEvent
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.PacketUtils.sendGroupedPacket
import world.cepi.kstom.util.playSound
import java.util.concurrent.ThreadLocalRandom

fun Player.showFirework(instance: Instance, position: Pos, effects: MutableList<FireworkEffect>) = listOf(this).showFirework(instance, position, effects)
fun Player.showFireworkWithDuration(instance: Instance, position: Pos, ticks: Int, effects: MutableList<FireworkEffect>) = listOf(this).showFireworkWithDuration(instance, position, ticks, effects)

fun Collection<Player>.showFirework(
    instance: Instance,
    position: Pos,
    effects: MutableList<FireworkEffect>
) {
    val fireworkMeta = FireworkMeta.Builder().effects(effects).build()
    val fireworkItemStack = ItemStack.builder(Material.FIREWORK_ROCKET).meta(fireworkMeta).build()
    val firework = Entity(EntityType.FIREWORK_ROCKET)
    val meta = firework.entityMeta as FireworkRocketMeta

    meta.fireworkInfo = fireworkItemStack
    firework.updateViewableRule { this.contains(it) }
    firework.setNoGravity(true)
    firework.setInstance(instance, position)

    sendGroupedPacket(this, EntityStatusPacket(firework.entityId, 17))

    firework.remove()
}

fun Collection<Player>.showFireworkWithDuration(
    instance: Instance,
    position: Pos,
    ticks: Int,
    effects: MutableList<FireworkEffect>
) {
    val fireworkMeta = FireworkMeta.Builder().effects(effects).build()
    val fireworkItemStack = ItemStack.builder(Material.FIREWORK_ROCKET).meta(fireworkMeta).build()
    val firework = Entity(EntityType.FIREWORK_ROCKET)
    val meta = firework.entityMeta as FireworkRocketMeta

    val rand = ThreadLocalRandom.current()

    meta.fireworkInfo = fireworkItemStack
    firework.updateViewableRule { this.contains(it) }
    firework.setNoGravity(true)
    firework.velocity = Vec(rand.nextDouble(0.02), 1.0, rand.nextDouble(0.02))

    this.forEach {
        it.playSound(Sound.sound(SoundEvent.ENTITY_FIREWORK_ROCKET_LAUNCH, Sound.Source.AMBIENT, 1f, 1f), position)
    }

    firework.setInstance(instance, position)

    firework.scheduler().submitTask {
        if (firework.aliveTicks > ticks) {
            sendGroupedPacket(this@showFireworkWithDuration, EntityStatusPacket(firework.entityId, 17))
            firework.remove()
            return@submitTask TaskSchedule.stop()
        }

        // acceleration
        firework.velocity = firework.velocity.apply { x, y, z -> Vec(x * 1.15, y + 0.8, z * 1.15) }

        return@submitTask TaskSchedule.nextTick()
    }
}