package dev.emortal.immortal.game

import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.event.PlayerJoinGameEvent
import dev.emortal.immortal.util.reset
import dev.emortal.immortal.util.resetTeam
import dev.emortal.immortal.util.safeSetInstance
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockPlaceEvent
import net.minestom.server.sound.SoundEvent
import org.tinylog.kotlin.Logger
import world.cepi.kstom.event.listenOnly

abstract class LobbyGame(gameOptions: GameOptions) : Game(gameOptions) {

    var allowBlockModification = false

   init {
       gameState = GameState.PLAYING

       eventNode.listenOnly<PlayerBlockPlaceEvent> {
           isCancelled = !allowBlockModification
       }
       eventNode.listenOnly<PlayerBlockBreakEvent> {
           isCancelled = !allowBlockModification
       }
   }

    override fun start() {
        playSound(Sound.sound(SoundEvent.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

        startingTask = null
        scoreboard?.updateLineContent("infoLine", Component.empty())

        if (gameTypeInfo.whenToRegisterEvents == WhenToRegisterEvents.GAME_START) registerEvents()
        gameStarted()
    }

    // Spectating is not supported for lobby games.
    override fun addSpectator(player: Player) {}
    override fun removeSpectator(player: Player) {}

    // Lobby does not have a countdown
    override fun startCountdown() {}
    override fun cancelCountdown() {}

    // Player count used for compass and NPCs, Lobby does not appear there
    override fun refreshPlayerCount() {}

    // Lobby cannot be won
    override fun victory(winningPlayers: Collection<Player>) {}

    var firstJoin = true

    override fun addPlayer(player: Player, joinMessage: Boolean) {
        if (firstJoin) {
            start()
        }
        firstJoin = false
        if (players.contains(player)) {
            Logger.warn("Contains player")
            return
        }
        queuedPlayers.remove(player)

        Logger.info("${player.username} joining game '${gameTypeInfo.name}'")

        players.add(player)

        if (joinMessage) sendMessage(
            Component.text()
                .append(Component.text("JOIN", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.username, NamedTextColor.GREEN))
                .append(Component.text(" joined the game ", NamedTextColor.GRAY))
        )

        player.respawnPoint = spawnPosition

        player.safeSetInstance(instance, spawnPosition)?.thenRun {
            player.reset()
            player.resetTeam()
            scoreboard?.addViewer(player)

            playSound(Sound.sound(SoundEvent.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1f, 1.2f))
            player.clearTitle()
            player.sendActionBar(Component.empty())

            playerJoin(player)

            EventDispatcher.call(PlayerJoinGameEvent(this, player))
        }
    }

    override fun getPlayers(): MutableCollection<Player> = players

    override fun canBeJoined(player: Player): Boolean {
        if (players.contains(player)) return false
        if (!queuedPlayers.contains(player) && players.size + queuedPlayers.size >= gameOptions.maxPlayers) {
            return false
        }
        return true
    }

}
