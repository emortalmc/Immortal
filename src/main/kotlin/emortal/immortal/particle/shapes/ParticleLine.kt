package emortal.immortal.particle.shapes

import net.minestom.server.coordinate.Vec
import net.minestom.server.network.packet.server.play.ParticlePacket
import world.cepi.kstom.util.component1
import world.cepi.kstom.util.component2
import world.cepi.kstom.util.component3

class ParticleLine(val particleSingle: ParticleSingle, val from: Vec, val to: Vec, val spacing: Double = 0.25) : ParticleShape {

    override val packets: Collection<ParticlePacket>
        get() {
            val collection = mutableListOf<ParticlePacket>()

            val (x, y, z) = from
            particleSingle.x = x
            particleSingle.y = y
            particleSingle.z = z

            val maxDistance = from.distance(to)
            val (dirX, dirY, dirZ) = from.sub(to).normalize().mul(spacing)

            var step = 0.0

            while (maxDistance > step) {
                particleSingle.x += dirX
                particleSingle.y += dirY
                particleSingle.z += dirZ
                step += spacing
                collection.addAll(particleSingle.packets)
            }

            return collection
        }

}