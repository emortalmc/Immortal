package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.command.arguments.literal
import world.cepi.kstom.command.kommand.Kommand

object SettingsCommand : Kommand({

    onlyPlayers()

    val maxplayers by literal
    val minplayers by literal

    val value = ArgumentType.Integer("value")

    condition {
        sender.hasLuckPermission("immortal.settings")
    }

    playerCallbackFailMessage = {
        it.sendMessage(Component.text("No permission", NamedTextColor.RED))
    }

    default {

    }

    syntax(maxplayers, value) {
        val game = player.game

        if (game == null) {
            player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED))
            return@syntax
        }

        game.gameOptions = game.gameOptions.copy(maxPlayers = !value)
        game.sendMessage(Component.text("${player.username} set max players to ${!value}", NamedTextColor.GOLD))
    }

    syntax(minplayers, value) {
        val game = player.game

        if (game == null) {
            player.sendMessage(Component.text("You are not in a game", NamedTextColor.RED))
            return@syntax
        }

        game.gameOptions = game.gameOptions.copy(minPlayers = !value)
        game.sendMessage(Component.text("${player.username} set min players to ${!value}", NamedTextColor.GOLD))
    }

}, "settings", "options")