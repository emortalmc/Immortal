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

    fun packet(particle: Particle, pos: Point, spread: Point = Vec.ZERO, count: Int = 1, data: Float = 0f, writer: BinaryWriter? = null): ParticlePacket {
        return ParticleCreator.createParticlePacket(particle, true, pos.x(), pos.y(), pos.z(), spread.x().toFloat(), spread.y().toFloat(), spread.z().toFloat(), data, count)  {
            if (writer != null) {
                it.write(writer)
            }
        }
    }

    fun particle(particle: Particle, pos: Point, spread: Point = Vec.ZERO, count: Int = 1, data: Float = 0f, writer: BinaryWriter? = null): ParticleSingle {
        return ParticleSingle(particle, pos, spread, count, data, writer)
    }

    /**
     * Creates the vibration effect created from skulk sensors
     */
    fun vibration(startingPosition: Point, endingPosition: Point, ticksToMove: Int): ParticleSingle {
        val writer = BinaryWriter()
        writer.writeDouble(startingPosition.x())
        writer.writeDouble(startingPosition.y())
        writer.writeDouble(startingPosition.z())
        writer.writeDouble(endingPosition.x())
        writer.writeDouble(endingPosition.y())
        writer.writeDouble(endingPosition.z())
        writer.writeInt(ticksToMove)

        return ParticleSingle(Particle.VIBRATION, Vec.ZERO, writer = writer, count = 0)
    }

    /**
     * Not all particles are colorable. Colorable particles:
     * @see Particle.AMBIENT_ENTITY_EFFECT
     * @see Particle.ENTITY_EFFECT
     * @see Particle.DUST
     * @see Particle.DUST_COLOR_TRANSITION
     * @see Particle.FALLING_DUST
     */
    fun colored(particle: Particle, pos: Point, startingColor: RGBLike, endingColor: RGBLike? = null, size: Float = 1f, spread: Point = Vec.ZERO, count: Int = 1, data: Float = 0f): ParticleSingle {
        if (particle != Particle.AMBIENT_ENTITY_EFFECT && particle == Particle.ENTITY_EFFECT && particle == Particle.DUST && particle == Particle.DUST_COLOR_TRANSITION && particle == Particle.FALLING_DUST) {
            throw Error("That particle is not colorable, see documentation")
        }

        val writer = BinaryWriter()
        writer.writeFloat(startingColor.red() / 255f)
        writer.writeFloat(startingColor.green() / 255f)
        writer.writeFloat(startingColor.blue() / 255f)
        writer.writeFloat(size)
        if (endingColor != null) {
            writer.writeFloat(endingColor.red() / 255f)
            writer.writeFloat(endingColor.green() / 255f)
            writer.writeFloat(endingColor.blue() / 255f)
        }

        return ParticleSingle(particle, pos, spread, count, data, writer)
    }

    fun moving(particle: Particle, pos: Point, velocity: Vec, speed: Float = 0.5f, writer: BinaryWriter? = null): ParticleSingle {
        return ParticleSingle(particle, pos, velocity, 0, speed, writer)
    }

    fun line(particle: Particle, startingPosition: Vec, endingPosition: Vec, spread: Point = Vec.ZERO, data: Float = 0f, writer: BinaryWriter? = null): ParticleLine {
        return ParticleLine(ParticleSingle(particle, startingPosition, spread, 1, data, writer), startingPosition, endingPosition)
    }
    fun line(particleSingle: ParticleSingle, startingPosition: Vec, endingPosition: Vec): ParticleLine {
        return ParticleLine(particleSingle, startingPosition, endingPosition)
    }

}