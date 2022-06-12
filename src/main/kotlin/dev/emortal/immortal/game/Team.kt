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
    val colour: RGBLike = NamedTextColor.WHITE,
    val collisionRule: TeamsPacket.CollisionRule = TeamsPacket.CollisionRule.NEVER,
    val nameTagVisibility: TeamsPacket.NameTagVisibility = TeamsPacket.NameTagVisibility.ALWAYS,
    var friendlyFire: Boolean = false,
    var canSeeInvisiblePlayers: Boolean = false
) : PacketGroupingAudience {

    private val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()

    val scoreboardTeam = Manager.team.createBuilder(teamName)
        .teamColor(NamedTextColor.nearestTo(TextColor.color(colour)))
        .collisionRule(collisionRule)
        .nameTagVisibility(nameTagVisibility)
        .also {
            if (friendlyFire) it.allowFriendlyFire()
            if (canSeeInvisiblePlayers) it.seeInvisiblePlayers()
        }
        .updateTeamPacket()
        .build()

    fun add(player: Player) {
        player.team = scoreboardTeam
        players.add(player)
    }

    fun remove(player: Player) {
        player.team = null
        players.remove(player)
    }

    fun destroy() {
        players.forEach {
            it.team = null
        }
        players.clear()
        Manager.team.deleteTeam(scoreboardTeam)
    }

    override fun getPlayers(): MutableCollection<Player> = players
}