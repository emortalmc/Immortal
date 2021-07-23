package emortal.immortal.particle

import emortal.immortal.particle.shapes.ParticleLine
import emortal.immortal.particle.shapes.ParticleSingle
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
    fun vibration(startingPosition: Point, endingPosition: Point): ParticleSingle {
        return ParticleSingle(Particle.VIBRATION, startingPosition, endingPosition)
    }

    /**
     * Not all particles are colorable. Colorable particles:
     * @see Particle.AMBIENT_ENTITY_EFFECT
     * @see Particle.ENTITY_EFFECT
     * @see Particle.DUST
     * @see Particle.DUST_COLOR_TRANSITION
     * @see Particle.FALLING_DUST
     */
    fun colored(particle: Particle, pos: Point, spread: Point, count: Int = 1, data: Float = 0f, writer: BinaryWriter? = null): ParticleSingle {
        if (particle != Particle.AMBIENT_ENTITY_EFFECT && particle == Particle.ENTITY_EFFECT && particle == Particle.DUST && particle == Particle.DUST_COLOR_TRANSITION && particle == Particle.FALLING_DUST) {
            throw Error("That particle is not colorable, see documentation")
        }

        return ParticleSingle(particle, pos, spread, count, data, writer)
    }

    fun moving(particle: Particle, pos: Point, velocity: Vec, speed: Float = 0.5f, writer: BinaryWriter?): ParticleSingle {
        return ParticleSingle(particle, pos, velocity, 1, speed, writer)
    }

    fun line(particle: Particle, startingPosition: Vec, endingPosition: Vec, spread: Point, data: Float, writer: BinaryWriter?): ParticleLine {
        return ParticleLine(ParticleSingle(particle, startingPosition, spread, 1, data, writer), startingPosition, endingPosition)
    }
    fun line(particleSingle: ParticleSingle, startingPosition: Vec, endingPosition: Vec): ParticleLine {
        return ParticleLine(particleSingle, startingPosition, endingPosition)
    }

}