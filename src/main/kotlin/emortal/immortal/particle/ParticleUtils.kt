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

    fun packet(particle: Particle, pos: Point, spread: Point = Vec.ZERO, count: Int = 1, data: Float = 0f, writer: (BinaryWriter) -> Unit = { }): ParticlePacket {
        return ParticleCreator.createParticlePacket(particle, true, pos.x(), pos.y(), pos.z(), spread.x().toFloat(), spread.y().toFloat(), spread.z().toFloat(), data, count)  {
            writer.invoke(it)
        }
    }

    fun particle(particle: Particle, pos: Point, spread: Point = Vec.ZERO, count: Int = 1, data: Float = 0f, writer: (BinaryWriter) -> Unit = { }): ParticleSingle {
        return ParticleSingle(particle, pos, spread, count, data, writer)
    }

    fun block(blockStateId: Int, pos: Point): ParticleSingle {
        return ParticleSingle(Particle.BLOCK, pos, writer = { it.writeVarInt(blockStateId) }, count = 1)
    }

    /**
     * Creates the vibration effect created from skulk sensors
     *
     * CURRENTLY NOT WORKING
     */
    fun vibration(startingPosition: Point, endingPosition: Point, ticksToMove: Int): ParticleSingle {
        return ParticleSingle(Particle.VIBRATION, Vec.ZERO, writer = {
            it.writeDouble(startingPosition.x())
            it.writeDouble(startingPosition.y())
            it.writeDouble(startingPosition.z())
            it.writeDouble(endingPosition.x())
            it.writeDouble(endingPosition.y())
            it.writeDouble(endingPosition.z())
            it.writeInt(ticksToMove)
        }, count = 1)
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


        return ParticleSingle(particle, pos, spread, count, data) {
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

    fun moving(particle: Particle, pos: Point, velocity: Vec, speed: Float = 0.5f, writer: (BinaryWriter) -> Unit = { }): ParticleSingle {
        return ParticleSingle(particle, pos, velocity, 0, speed, writer)
    }

    fun line(particle: Particle, startingPosition: Vec, endingPosition: Vec, spread: Point = Vec.ZERO, data: Float = 0f, writer: (BinaryWriter) -> Unit = { }): ParticleLine {
        return ParticleLine(ParticleSingle(particle, startingPosition, spread, 1, data, writer), startingPosition, endingPosition)
    }
    fun line(particleSingle: ParticleSingle, startingPosition: Vec, endingPosition: Vec): ParticleLine {
        return ParticleLine(particleSingle, startingPosition, endingPosition)
    }

}