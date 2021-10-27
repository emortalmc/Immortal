package dev.emortal.immortal.game

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import net.minestom.server.entity.Entity
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import java.time.Duration

abstract class PvpGame(gameOptions: GameOptions) : Game(gameOptions) {

    fun kill(player: Player, killer: Entity?) {

        val subtitle = Component.empty()

        player.clearEffects()
        player.heal()
        player.isInvisible = true
        player.gameMode = GameMode.SPECTATOR
        player.showTitle(
            Title.title(
                Component.text("YOU DIED", NamedTextColor.RED, TextDecoration.BOLD),
                subtitle,
                Title.Times.of(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1))
            )
        )

        playerDied(player, killer)
    }

    abstract fun playerDied(player: Player, killer: Entity?)

    abstract fun respawn(player: Player)

}