package emortal.immortal.util

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.item.Enchantment
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

fun Player.takeKnockback(attacker: Player) {

    val d0 = attacker.position.x() - position.x()
    val d1 = attacker.position.z() - position.z()

    val magnitude = sqrt(d0 * d0 + d1 * d1)

    val knockbackLevel = attacker.itemInMainHand.meta.enchantmentMap[Enchantment.KNOCKBACK] ?: 0
    var i = knockbackLevel.toInt()

    if (attacker.isSprinting) ++i


    var newVelocity = velocity
        .withX(((velocity.x() / 2) - (d0 / magnitude * 0.4)))
        .withY((min((velocity.y() / 2) + 0.4, 0.4)))
        .withZ(((velocity.z() / 2) - (d1 / magnitude * 0.4)))


    if (i > 0) newVelocity = newVelocity.add(
        Vec(
            (-sin(attacker.position.yaw() * Math.PI / 180.0f) * i.toFloat() * 0.5),
            1.0,
            (cos(attacker.position.yaw() * Math.PI / 180.0f) * i.toFloat() * 0.4)
        )
    )


    println("x${newVelocity.x()} y${newVelocity.y()} z${newVelocity.z()}")

    velocity = newVelocity

}
