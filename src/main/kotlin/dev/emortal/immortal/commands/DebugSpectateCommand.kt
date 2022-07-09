package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import net.minestom.server.command.builder.arguments.ArgumentWord
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand

// Only registered with debug mode, no worries with permissions
object DebugSpectateCommand : Kommand({

    val playerArg = ArgumentWord("player")

    syntax(playerArg) {
        val other = Manager.connection.findPlayer(!playerArg) ?: return@syntax

        player.scheduleNextTick {
            player.joinGame(other.game!!, spectate = true)
        }
    }

}, "dspectate")