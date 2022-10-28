package dev.emortal.immortal.game

import dev.emortal.immortal.config.GameOptions
import net.minestom.server.entity.Player
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import world.cepi.kstom.event.listenOnly

abstract class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {

    var allowBlockModification = false

    init {
        instanceFuture.thenAcceptAsync { instance ->
            instance.eventNode().listenOnly<PlayerBlockPlaceEvent> {
                isCancelled = !allowBlockModification
            }
            instance.eventNode().listenOnly<PlayerBlockBreakEvent> {
                isCancelled = !allowBlockModification
            }

            start()
        }
    }

    // Lobby does not have a countdown
    override fun startCountdown() {}
    override fun cancelCountdown() {}

    // Player count used for compass and NPCs, Lobby does not appear there
    override fun refreshPlayerCount() {}

    // Lobby cannot be won
    override fun victory(winningPlayers: Collection<Player>) {}

    override fun getPlayers(): MutableCollection<Player> = players

    override fun canBeJoined(player: Player): Boolean {
        if (players.contains(player)) return false
        if (!queuedPlayers.contains(player) && players.size + queuedPlayers.size >= gameOptions.maxPlayers) {
            return false
        }
        return true
    }

}
