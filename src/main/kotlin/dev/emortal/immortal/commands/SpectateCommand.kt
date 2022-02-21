package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.spectateGame
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import kotlinx.coroutines.Dispatchers
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.arguments.ArgumentWord
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

object SpectateCommand : Kommand({

    onlyPlayers

    val friendArg = ArgumentWord("friend")
        .map { Manager.connection.getPlayer(it) ?: throw ArgumentSyntaxException("Invalid player", it, 1) }
        /*.suggestComplex { runBlocking {
            this@suggestComplex.player.getFriendsAsync()
                .mapNotNull { Manager.connection.getPlayer(it) }
                .map { SuggestionEntry(it.username, Component.text("friend")) }
            }
        }*/

    friendArg.failCallback {
        sender.sendMessage(Component.text("Invalid player", NamedTextColor.RED))
    }

    syntaxSuspending(Dispatchers.IO, friendArg) {
        val friend = !friendArg
        val game = friend.game

        //if (!player.hasPermission("immortal.spectate.admin") && !player.getFriendsAsync().contains(friend.uuid)) {
        //    player.sendMessage(Component.text("You are not friends with '${friend.username}'", NamedTextColor.RED))
        //    return@syntaxSuspending
        //}

        if (game == null) {
            player.sendMessage(Component.text("'${friend.username}' is not in a game", NamedTextColor.RED))
            return@syntaxSuspending
        }

        if (!player.hasLuckPermission("immortal.spectate.admin") && !game.gameTypeInfo.spectatable) {
            player.sendMessage(Component.text("${friend.username}'s game can not be spectated", NamedTextColor.RED))
            return@syntaxSuspending
        }

        //player.sendMessage(Component.text("Spectating is currently disabled", NamedTextColor.RED))
        player.spectateGame(game)
    }

}, "spectate")