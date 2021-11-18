package dev.emortal.immortal.util

import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.item.Enchantment
import kotlin.math.*

fun Entity.takeKnockback(attacker: Entity) {
    val horizontalKnockback = 0.25 * 20.0

    val d0 = attacker.position.x() - position.x()
    val d1 = attacker.position.z() - position.z()

    val magnitude = sqrt(d0 * d0 + d1 * d1)

    var newVelocity = velocity
        .withX(((velocity.x() / 2) - (d0 / magnitude * horizontalKnockback)))
        .withY((min((velocity.y() / 2) + 8, 8.0)))
        .withZ(((velocity.z() / 2) - (d1 / magnitude * horizontalKnockback)))

    if (newVelocity.y() > 8)
        newVelocity = newVelocity.withY(8.0);

    velocity = newVelocity
}

fun Entity.takeKnockback(attacker: Player) {
    val verticalKnockback = 0.4 * 20
    val horizontalKnockback = 0.4 * 20
    val extraHorizontalKnockback = 0.3 * 20
    val extraVerticalKnockback = 0.1 * 20
    val limitVerticalKnockback = 0.4 * 20

    val distanceX = attacker.position.x() - position.x()
    val distanceY = attacker.position.z() - position.z()

    val magnitude = attacker.position.distance(position)

    val knockbackLevel = attacker.itemInMainHand.meta.enchantmentMap[Enchantment.KNOCKBACK] ?: 0
    var i = knockbackLevel.toDouble()

    if (attacker.isSprinting) i += 1.25

    var newVelocity = velocity
        .withX { x -> (x / 2) - (distanceX / magnitude * horizontalKnockback) }
        .withY { y -> (y / 2) + verticalKnockback }
        .withZ { z -> (z / 2) - (distanceY / magnitude * horizontalKnockback) }

    if (i > 0) newVelocity = newVelocity.add(
        -sin(attacker.position.yaw() * PI / 180.0f) * i.toFloat() * extraHorizontalKnockback,
        extraVerticalKnockback,
        cos(attacker.position.yaw() * PI / 180.0f) * i.toFloat() * extraHorizontalKnockback
    )

    if (newVelocity.y() > limitVerticalKnockback) newVelocity = newVelocity.withY(limitVerticalKnockback)

    velocity = newVelocity
}


