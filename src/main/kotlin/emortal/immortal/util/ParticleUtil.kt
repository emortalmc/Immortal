package emortal.immortal.util

import net.kyori.adventure.util.RGBLike
import net.minestom.server.Viewable
import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.utils.PacketUtils
import net.minestom.server.utils.binary.BinaryWriter

/**
 * Sends a particle to a specific players
 *
 * @see sendParticleToViewers
 * @see sendParticleToViewersAndSelf
 */
fun Player.sendParticle(
    particle: Particle,
    pos: Point,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    offsetZ: Float = 0f,
    count: Int = 1,
    data: Float = 0f,
    dataWriter: (BinaryWriter) -> Unit = { }
) = ParticleUtil.sendParticle(setOf(this), particle, pos, offsetX, offsetY, offsetZ, count, data, dataWriter)

/**
 * Try to use `Viewable.sendParticleToViewers` where possible
 *
 * e.g. sending a particle around a player - will only send the particle to people who will actually see it
 *
 * @see sendParticleToViewers
 * @see sendParticleToViewersAndSelf
 */
fun Instance.sendParticle(
    particle: Particle,
    pos: Point,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    offsetZ: Float = 0f,
    count: Int = 1,
    data: Float = 0f,
    dataWriter: (BinaryWriter) -> Unit = { }
) = ParticleUtil.sendParticle(this.players, particle, pos, offsetX, offsetY, offsetZ, count, data, dataWriter)

fun Viewable.sendParticleToViewers(
    particle: Particle,
    pos: Point,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    offsetZ: Float = 0f,
    count: Int = 1,
    data: Float = 0f,
    dataWriter: (BinaryWriter) -> Unit = { }
) {
    this.sendPacketToViewers(ParticleCreator.createParticlePacket(
        particle, false,
        pos.x(), pos.y(), pos.z(),
        offsetX, offsetY, offsetZ, data, count, dataWriter
    ))
}

fun Viewable.sendParticleToViewersAndSelf(
    particle: Particle,
    pos: Point,
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    offsetZ: Float = 0f,
    count: Int = 1,
    data: Float = 0f,
    dataWriter: (BinaryWriter) -> Unit = { }
) {
    this.sendPacketToViewersAndSelf(ParticleCreator.createParticlePacket(
        particle, false,
        pos.x(), pos.y(), pos.z(),
        offsetX, offsetY, offsetZ, data, count, dataWriter
    ))
}

object ParticleUtil {

    /**
     * Example: player.sendParticle(Particle.DUST_COLOR_TRANSITION, player.position, count = 0) {
     *      ParticleUtil.transitionParticleData(it, NamedTextColor.AQUA, NamedTextColor.DARK_AQUA, 1f)
     * }
     */
    fun transitionParticleData(
        writer: BinaryWriter,
        fromColour: RGBLike,
        toColour: RGBLike,
        size: Float
    ) {
        writer.writeFloat(fromColour.red() / 255f)
        writer.writeFloat(fromColour.green() / 255f)
        writer.writeFloat(fromColour.blue() / 255f)
        writer.writeFloat(size)
        writer.writeFloat(toColour.red() / 255f)
        writer.writeFloat(toColour.green() / 255f)
        writer.writeFloat(toColour.blue() / 255f)
    }

    /**
     * Example: player.sendParticle(Particle.DUST, player.position, count = 0) {
     *      ParticleUtil.colouredParticleData(it, NamedTextColor.AQUA, 1f)
     * }
     */
    fun colouredParticleData(
        writer: BinaryWriter,
        fromColour: RGBLike,
        size: Float
    ) {
        writer.writeFloat(fromColour.red() / 255f)
        writer.writeFloat(fromColour.green() / 255f)
        writer.writeFloat(fromColour.blue() / 255f)
        writer.writeFloat(size)
    }

    fun sendParticle(
        viewers: Set<Player>,
        particle: Particle,
        pos: Point,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        offsetZ: Float = 0f,
        count: Int = 1,
        data: Float = 0f,
        dataWriter: (BinaryWriter) -> Unit = { }
    ) {
        PacketUtils.sendGroupedPacket(
            viewers, ParticleCreator.createParticlePacket(
                particle, false,
                pos.x(), pos.y(), pos.z(),
                offsetX, offsetY, offsetZ, data, count, dataWriter
            )
        )
    }

}