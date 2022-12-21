package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player

internal object ShortenStartCommand : Command("shortstart") {

    init {
        setCondition { sender, _ ->
            sender.hasLuckPermission("immortal.forcestart")
        }

        setDefaultExecutor { sender, _ ->
            if (!sender.hasLuckPermission("immortal.forcestart")) {
                sender.sendMessage("No permission")
                return@setDefaultExecutor
            }

            val player = sender as? Player ?: return@setDefaultExecutor
            val playerGame = player.game

            if (playerGame == null) {
                player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED))
                return@setDefaultExecutor
            }
            if (playerGame.gameState != GameState.WAITING_FOR_PLAYERS) {
                player.sendMessage(Component.text("The game has already started", NamedTextColor.RED))
                return@setDefaultExecutor
            }

            playerGame.startingTask?.cancel()
            playerGame.startingSecondsLeft.set(4)
            playerGame.sendMessage(Component.text("${player.username} shortened the countdown", NamedTextColor.GOLD))
        }
    }

}