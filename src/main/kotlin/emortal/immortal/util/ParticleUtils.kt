package emortal.immortal.util

import net.minestom.server.color.Color
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.utils.PacketUtils
import net.minestom.server.utils.Position
import net.minestom.server.utils.Vector
import net.minestom.server.utils.binary.BinaryWriter

fun Player.sendParticle(
    particle: Particle,
    pos: Position,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int
) = sendParticle(particle, pos, offsetX, offsetY, offsetZ, count, 0f)

fun Instance.sendParticle(
    particle: Particle,
    pos: Position,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int
) = sendParticle(particle, pos, offsetX, offsetY, offsetZ, count, 0f)

fun Player.sendParticle(
    particle: Particle,
    pos: Position,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int,
    data: Float
) {
    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            particle, false,
            pos.x, pos.y, pos.z,
            offsetX, offsetY, offsetZ, data, count, null
        )
    )
}

fun Instance.sendParticle(
    particle: Particle,
    pos: Position,
    offsetX: Float,
    offsetY: Float,
    offsetZ: Float,
    count: Int,
    data: Float
) {
    PacketUtils.sendGroupedPacket(this.players, ParticleCreator.createParticlePacket(
        particle, false,
        pos.x, pos.y, pos.z,
        offsetX, offsetY, offsetZ, data, count, null
    ))
}

/**
 * Sends the player a coloured particle
 * @param size A float that goes between `0.01f` and `4f`
 */
fun Player.sendColouredTransitionParticle(
    pos: Position,
    fromColour: Color,
    toColour: Color,
    size: Float
) {
    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST_COLOR_TRANSITION, false,
            pos.x, pos.y, pos.z,
            0f, 0f, 0f, 0f, 1
        ) { writer: BinaryWriter ->
            run {
                writer.writeFloat(fromColour.red / 255f)
                writer.writeFloat(fromColour.green / 255f)
                writer.writeFloat(fromColour.blue / 255f)
                writer.writeFloat(size)
                writer.writeFloat(toColour.red / 255f)
                writer.writeFloat(toColour.green / 255f)
                writer.writeFloat(toColour.blue / 255f)
            }
        }
    )
}

fun Player.sendColouredParticle(
    pos: Position,
    fromColour: Color,
    size: Float
) {
    playerConnection.sendPacket(
        ParticleCreator.createParticlePacket(
            Particle.DUST, false,
            pos.x, pos.y, pos.z,
            0f, 0f, 0f, 0f, 1
        ) { writer: BinaryWriter ->
            run {
                writer.writeFloat(fromColour.red / 255f)
                writer.writeFloat(fromColour.green / 255f)
                writer.writeFloat(fromColour.blue / 255f)
                writer.writeFloat(size)
            }
        }
    )
}

fun Player.sendMovingParticle(
    particle: Particle,
    pos: Position,
    vector: Vector,
    speed: Float
) = sendParticle(particle, pos, vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat(), 0, speed)

fun Instance.sendMovingParticle(
    particle: Particle,
    pos: Position,
    vector: Vector,
    speed: Float
) = sendParticle(particle, pos, vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat(), 0, speed)