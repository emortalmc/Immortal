package dev.emortal.immortal.commands

import dev.emortal.acquaintance.RelationshipManager.friends
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentWord
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import net.minestom.server.command.builder.suggestion.SuggestionEntry
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.suggestComplex
import world.cepi.kstom.command.kommand.Kommand

object SpectateCommand : Kommand({

    onlyPlayers

    val friendArg = ArgumentWord("friend")
        .map { Manager.connection.getPlayer(it) ?: throw ArgumentSyntaxException("Invalid player", it, 1) }
        .suggestComplex {
            this.player.friends
                .mapNotNull { Manager.connection.getPlayer(it) }
                .map { SuggestionEntry(it.username, Component.text("friend")) }
        }

    friendArg.failCallback {
        sender.sendMessage(Component.text("Invalid player", NamedTextColor.RED))
    }

    syntax(friendArg) {
        val friend = !friendArg
        val game = friend.game

        if (!player.friends.contains(friend.uuid)) {
            player.sendMessage(Component.text("You are not friends with '${friend.username}'", NamedTextColor.RED))
            return@syntax
        }

        if (game == null) {
            player.sendMessage(Component.text("'${friend.username}' is not in a game", NamedTextColor.RED))
            return@syntax
        }

        if (game.gameTypeInfo.gameName == "lobby") {
            player.sendMessage(Component.text("'${friend.username}' is in the lobby", NamedTextColor.RED))
            return@syntax
        }

        game.addSpectator(player, friend)
    }

}, "spectate")