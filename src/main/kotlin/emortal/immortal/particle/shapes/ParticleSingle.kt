package emortal.immortal.particle.shapes

import emortal.immortal.particle.ParticleUtils
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.utils.binary.BinaryWriter

class ParticleSingle(
    val particle: Particle,
    var x: Double,
    var y: Double,
    var z: Double,
    val xSpread: Float = 0f,
    val ySpread: Float = 0f,
    val zSpread: Float = 0f,
    val count: Int = 1,
    val data: Float = 0f,
    val writer: (BinaryWriter) -> Unit = { }
) : ParticleShape {

    override val packets: Collection<ParticlePacket>
        get() = listOf(ParticleUtils.packet(particle, x, y, z, xSpread, ySpread, zSpread, count, data, writer))

}