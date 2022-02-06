package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.inventory.GameSelectorGUI
import net.kyori.adventure.sound.Sound
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.sound.SoundEvent
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand

object PlayCommand : Kommand({

    onlyPlayers

    val gamemodeArg = ArgumentType.Word("gamemode").map { input: String ->
        GameManager.registeredGameMap.values.firstOrNull { it.gameName == input && it.showsInSlashPlay }
    }.suggest {
        GameManager.registeredGameMap.values
            .filter { it.showsInSlashPlay }
            .map { it.gameName }
    }

    syntax(gamemodeArg) {
        val gamemode = !gamemodeArg ?: return@syntax

        player.joinGameOrNew(gamemode.gameName)
    }

    default {
        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1f, 2f))
        player.openInventory(GameSelectorGUI.inventory)
    }

}, "play")