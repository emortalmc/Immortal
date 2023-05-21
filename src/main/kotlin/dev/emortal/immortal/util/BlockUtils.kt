package dev.emortal.immortal.util

import net.minestom.server.Viewable
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.coordinate.Point
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.server.play.BlockBreakAnimationPacket
import net.minestom.server.network.packet.server.play.EffectPacket

fun PacketGroupingAudience.sendBlockDamage(destroyStage: Byte, point: Point, entityID: Int) {
    val packet = BlockBreakAnimationPacket(entityID, point, destroyStage)
    sendGroupedPacket(packet)
}

fun PacketGroupingAudience.sendBreakBlockEffect(point: Point, block: Block) {
    sendGroupedPacket(EffectPacket(2001 /*Block break + block break sound*/, point, block.stateId().toInt(), false))
}

fun Viewable.sendBlockDamage(destroyStage: Byte, point: Point, entityID: Int) {
    val packet = BlockBreakAnimationPacket(entityID, point, destroyStage)
    sendPacketToViewersAndSelf(packet)
}

fun Viewable.sendBreakBlockEffect(point: Point, block: Block) {
    sendPacketToViewersAndSelf(EffectPacket(2001 /*Block break + block break sound*/, point, block.stateId().toInt(), false))
}