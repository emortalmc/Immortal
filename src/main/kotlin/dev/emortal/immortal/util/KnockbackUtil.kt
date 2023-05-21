package dev.emortal.immortal.util

import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.Enchantment
import kotlin.math.*

fun Entity.takeKnockback(position: Pos, strength: Double = 1.0) {
    val horizontalKnockback = 0.25 * 20.0

    val d0 = position.x() - this.position.x()
    val d1 = position.z() - this.position.z()

    val magnitude = sqrt(d0 * d0 + d1 * d1) * strength

    var newVelocity = velocity
        .also {
            if (magnitude != 0.0) it
                .withX(((velocity.x() / 2) - (d0 / magnitude * horizontalKnockback)))
                .withZ(((velocity.z() / 2) - (d1 / magnitude * horizontalKnockback)))
        }
        .withY((min((velocity.y() / 2) + 8, 8.0)))

    if (newVelocity.y() > 8)
        newVelocity = newVelocity.withY(8.0)

    velocity = newVelocity
}

fun Entity.takeKnockback(attacker: Player, knockbackLevel: Double = attacker.itemInMainHand.meta().enchantmentMap[Enchantment.KNOCKBACK]?.toDouble() ?: 0.0) {
    val tps = MinecraftServer.TICK_PER_SECOND
    val horizontalKnockback = 0.4 * tps
    val verticalKnockback = 0.4 * tps
    val extraHorizontalKnockback = 0.5 * tps
    val extraVerticalKnockback = 0.1 * tps
    val limitVerticalKnockback = 0.5 * tps

    var d0: Double = attacker.position.x - this.position.x
    var d1: Double = attacker.position.z - this.position.z
    while (d0 * d0 + d1 * d1 < 1.0E-4) {
        d0 = (Math.random() - Math.random()) * 0.01
        d1 = (Math.random() - Math.random()) * 0.01
    }

    val magnitude = sqrt(d0 * d0 + d1 * d1)

    var i = knockbackLevel

    if (attacker.isSprinting) i += 1.0

    var newVelocity = velocity
        .withX { x -> (x / 2) - (d0 / magnitude * horizontalKnockback) }
        .withY { y -> (y / 2) + verticalKnockback }
        .withZ { z -> (z / 2) - (d1 / magnitude * horizontalKnockback) }

    if (newVelocity.y() > limitVerticalKnockback) newVelocity = newVelocity.withY(limitVerticalKnockback)

    if (i > 0) newVelocity = newVelocity.add(
        -sin(attacker.position.yaw * PI / 180.0f) * (i * extraHorizontalKnockback).toFloat(),
        extraVerticalKnockback,
        cos(attacker.position.yaw * PI / 180.0f) * (i * extraHorizontalKnockback).toFloat()
    )

    velocity = newVelocity
}


