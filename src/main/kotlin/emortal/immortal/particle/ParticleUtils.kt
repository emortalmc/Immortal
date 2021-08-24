package emortal.immortal.particle

import emortal.immortal.particle.shapes.ParticleLine
import emortal.immortal.particle.shapes.ParticleSingle
import net.kyori.adventure.util.RGBLike
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleCreator
import net.minestom.server.utils.binary.BinaryWriter

object ParticleUtils {

    fun packet(particle: Particle, x: Double, y: Double, z: Double, xSpread: Float = 0f, ySpread: Float = 0f, zSpread: Float = 0f, count: Int = 1, data: Float = 0f, writer: (BinaryWriter) -> Unit = { }): ParticlePacket {
        return ParticleCreator.createParticlePacket(particle, true, x, y, z, xSpread, ySpread, zSpread, data, count)  {
            writer.invoke(it)
        }
    }

    fun particle(particle: Particle, x: Double, y: Double, z: Double, xSpread: Float = 0f, ySpread: Float = 0f, zSpread: Float = 0f, count: Int = 1, data: Float = 0f, writer: (BinaryWriter) -> Unit = { }): ParticleSingle {
        return ParticleSingle(particle, x, y, z, xSpread, ySpread, zSpread, count, data, writer)
    }

    fun block(blockStateId: Int, x: Double, y: Double, z: Double): ParticleSingle {
        return ParticleSingle(Particle.BLOCK, x, y, z, writer = { it.writeVarInt(blockStateId) }, count = 1)
    }

    /**
     * Creates the vibration effect created from skulk sensors
     *
     * CURRENTLY NOT WORKING

    fun vibration(startingPosition: Point, endingPosition: Point, ticksToMove: Int): ParticleSingle {
        return ParticleSingle(Particle.VIBRATION, writer = {
            it.writeDouble(startingPosition.x())
            it.writeDouble(startingPosition.y())
            it.writeDouble(startingPosition.z())
            it.writeDouble(endingPosition.x())
            it.writeDouble(endingPosition.y())
            it.writeDouble(endingPosition.z())
            it.writeInt(ticksToMove)
        }, count = 1)
    }*/

    /**
     * Not all particles are colorable
     */
    fun colored(particle: Particle, x: Double, y: Double, z: Double, xSpread: Float = 0f, ySpread: Float = 0f, zSpread: Float = 0f, startingColor: RGBLike, endingColor: RGBLike? = null, size: Float = 1f, count: Int = 1, data: Float = 0f): ParticleSingle {
        return ParticleSingle(particle, x, y, z, xSpread, ySpread, zSpread, count, data) {
            it.writeFloat(startingColor.red() / 255f)
            it.writeFloat(startingColor.green() / 255f)
            it.writeFloat(startingColor.blue() / 255f)
            it.writeFloat(size)
            if (endingColor != null) {
                it.writeFloat(endingColor.red() / 255f)
                it.writeFloat(endingColor.green() / 255f)
                it.writeFloat(endingColor.blue() / 255f)
            }
        }
    }

    fun moving(particle: Particle, x: Double, y: Double, z: Double, velocity: Vec, speed: Float = 0.5f, writer: (BinaryWriter) -> Unit = { }): ParticleSingle {
        return ParticleSingle(particle, x, y, z, velocity.x().toFloat(), velocity.y().toFloat(), velocity.z().toFloat(), 0, speed, writer)
    }

    fun line(particleSingle: ParticleSingle, startingPosition: Vec, endingPosition: Vec, spacing: Double = 0.25): ParticleLine {
        return ParticleLine(particleSingle, startingPosition, endingPosition, spacing)
    }

}