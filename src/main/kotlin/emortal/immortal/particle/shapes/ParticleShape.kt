package emortal.immortal.particle.shapes

import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.network.packet.server.play.ParticlePacket
import net.minestom.server.utils.PacketUtils

fun Instance.sendParticle(particleShape: ParticleShape)
        = particleShape.packets.forEach { _ -> players.sendParticle(particleShape) }
fun Player.sendParticle(particleShape: ParticleShape)
        = particleShape.packets.forEach { playerConnection.sendPacket(it) }
fun Collection<Player>.sendParticle(particleShape: ParticleShape)
        = particleShape.packets.forEach { PacketUtils.sendGroupedPacket(this, it) }

interface ParticleShape {

    val packets: Collection<ParticlePacket>

    fun render(instance: Instance)
            = packets.forEach { _ -> render(instance.players) }
    fun render(player: Player)
            = packets.forEach { player.playerConnection.sendPacket(it)}
    fun render(players: Collection<Player>)
            = packets.forEach { PacketUtils.sendGroupedPacket(players, it) }

}