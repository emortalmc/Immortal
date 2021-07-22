package emortal.immortal

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameManager.joinGameOrNew
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import world.cepi.kstom.command.addSyntax
import world.cepi.kstom.command.arguments.suggest

object PlayCommand : Command("play") {

    init {
        val gamemode = ArgumentType.Word("gamemode").suggest {
            GameManager.registeredGameMap.values
                .map { it.gameName }
        }.map { input: String ->
            GameManager.registeredGameMap.entries.firstOrNull { it.value.gameName == input }
                ?: throw ArgumentSyntaxException("Invalid game name", input, 1)

        }

        addSyntax(gamemode) {
            val gamemode = context.get(gamemode)

            sender.asPlayer().joinGameOrNew(gamemode.key)
        }
    }

}