package dev.emortal.immortal.game

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.RGBLike
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.TeamsPacket
import net.minestom.server.scoreboard.Team
import world.cepi.kstom.Manager

class Team(
    val teamName: String,
    val colour: RGBLike,
    val players: List<Player>,
    val maxPlayers: Int = Integer.MAX_VALUE,
    val collisionRule: TeamsPacket.CollisionRule = TeamsPacket.CollisionRule.NEVER,
    val nameTagVisibility: TeamsPacket.NameTagVisibility = TeamsPacket.NameTagVisibility.ALWAYS
) {
    val team: Team = Manager.team.createTeam(teamName).also {
        it.teamColor = NamedTextColor.nearestTo(TextColor.color(colour))
        it.collisionRule = collisionRule
        it.nameTagVisibility = nameTagVisibility

        players.forEach { player -> player.team = it }
    }
}