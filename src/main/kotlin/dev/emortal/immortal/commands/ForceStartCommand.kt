package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameState
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import world.cepi.kstom.command.kommand.Kommand

object ForceStartCommand : Kommand({

    condition {
        sender.hasLuckPermission("immortal.forcestart")
    }

    playerCallbackFailMessage = {
        it.sendMessage(Component.text("No permission", NamedTextColor.RED))
    }

    onlyPlayers

    default {

        val playerGame = player.game

        if (playerGame == null) {
            player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED))
            return@default
        }
        if (playerGame.gameState != GameState.WAITING_FOR_PLAYERS) {
            player.sendMessage(Component.text("The game has already started", NamedTextColor.RED))
            return@default
        }

        playerGame.startingTask?.cancel()
        playerGame.start()
        playerGame.sendMessage(Component.text("${player.username} started the game early", NamedTextColor.GOLD))
    }

}, "forcestart")