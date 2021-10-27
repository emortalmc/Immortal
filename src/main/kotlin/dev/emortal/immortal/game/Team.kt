package dev.emortal.immortal.game

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.RGBLike
import net.minestom.server.adventure.audience.PacketGroupingAudience
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import world.cepi.kstom.Manager
import java.util.concurrent.ConcurrentHashMap

class Team(
    val teamName: String,
    val colour: RGBLike,
    val collisionRule: TeamsPacket.CollisionRule = TeamsPacket.CollisionRule.NEVER,
    val nameTagVisibility: TeamsPacket.NameTagVisibility = TeamsPacket.NameTagVisibility.ALWAYS
) : PacketGroupingAudience {

    private val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    val scoreboardTeam = Manager.team.createTeam(teamName).also {
        it.teamColor = NamedTextColor.nearestTo(TextColor.color(colour))
        it.collisionRule = collisionRule
        it.nameTagVisibility = nameTagVisibility
    }

    fun add(player: Player) {
        player.team = scoreboardTeam
        players += player
    }

    fun remove(player: Player) {
        player.team = null
        players -= player
    }

    fun has(player: Player) {
        players.contains(player)
    }

    override fun getPlayers(): MutableCollection<Player> = players

}