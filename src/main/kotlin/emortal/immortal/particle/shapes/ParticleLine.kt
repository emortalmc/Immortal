package emortal.immortal.particle.shapes

import net.minestom.server.coordinate.Vec
import net.minestom.server.network.packet.server.play.ParticlePacket

class ParticleLine(val particleSingle: ParticleSingle, val from: Vec, val to: Vec, val spacing: Double = 0.25) : ParticleShape {

    override val packets: Collection<ParticlePacket>
        get() {
            val collection = mutableListOf<ParticlePacket>()

            var currentPos = from
            val maxDistance = from.distance(to)
            val direction = from.sub(to).normalize().mul(spacing)

            var step = 0.0

            while (maxDistance > step) {
                collection.addAll(particleSingle.packets)
                currentPos = currentPos.add(direction)

                step += spacing
            }

            return collection
        }

}