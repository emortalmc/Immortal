package emortal.immortal

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import world.cepi.kstom.command.addSyntax
import world.cepi.kstom.command.arguments.suggest

object PlayCommand : Command("play") {

    init {
        val gamemodearg = ArgumentType.Word("gamemode").map { input: String ->
            GameManager.registeredGameMap.entries.firstOrNull { it.value.gameName == input }
                ?: throw ArgumentSyntaxException("Invalid game name", input, 1)

        }.suggest {
            GameManager.registeredGameMap.values
                .map { it.gameName }
        }

        addSyntax(gamemodearg) {
            val gamemode = context.get(gamemodearg)

            sender.asPlayer().joinGameOrNew(gamemode.key, GameManager.registeredGameMap[gamemode.key]!!.defaultGameOptions)
            sender.asPlayer().sendMessage(Component.text("Joining ${gamemode.value.gameName}...", NamedTextColor.GREEN))
        }
    }

}