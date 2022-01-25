package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.inventory.GameSelectorGUI
import dev.emortal.immortal.util.smallcaps
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import java.time.Duration

object PlayCommand : Kommand({

    onlyPlayers

    val gamemodeArg = ArgumentType.Word("gamemode").map { input: String ->
        GameManager.registeredGameMap.values.firstOrNull { it.gameName == input && it.showsInSlashPlay }
    }.suggest {
        GameManager.registeredGameMap.values
            .filter { it.showsInSlashPlay }
            .map { it.gameName }
    }

    val presetArg = ArgumentType.Word("preset")
        .map { input: String ->
            GameManager.registeredGameMap.values.map { it.gamePresets }.flatMap { it.keys }.firstOrNull { it == input }
        }
        .suggest {
            GameManager.registeredGameMap.values
                .filter { it.showsInSlashPlay }
                .map { it.gamePresets.keys }
                .flatten()
        }

    syntax(gamemodeArg) {
        val gamemode = !gamemodeArg ?: return@syntax

        val joinGameFuture = player.joinGameOrNew(gamemode.gameName)

        joinGameFuture.thenAccept {
            if (it) player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        }
    }

    syntax(gamemodeArg, presetArg) {
        val gamemode = !gamemodeArg ?: return@syntax
        val preset = !presetArg
        val options = gamemode.gamePresets[preset] ?: return@syntax

        val joinGameFuture = player.joinGameOrNew(gamemode.gameName, options)

        joinGameFuture.thenAccept {
            if (it) player.playSound(Sound.sound(SoundEvent.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
        }
    }

    default {
        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 2f))
        player.openInventory(GameSelectorGUI.inventory)
    }

}, "play")