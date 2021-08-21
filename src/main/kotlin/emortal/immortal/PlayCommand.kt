package emortal.immortal

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameManager.game
import emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentType
import net.minestom.server.command.builder.exception.ArgumentSyntaxException
import world.cepi.kstom.Manager
import world.cepi.kstom.command.addSyntax
import world.cepi.kstom.command.arguments.suggest
import java.time.Duration

object PlayCommand : Command("play") {

    init {
        val gamemodearg = ArgumentType.Word("gamemode").map { input: String ->
            GameManager.registeredGameMap.entries.firstOrNull { it.value.gameName == input && it.value.showsInPlayCommand }
                ?: throw ArgumentSyntaxException("Invalid game name", input, 1)

        }.suggest {
            GameManager.registeredGameMap.values
                .filter { it.showsInPlayCommand }
                .map { it.gameName }
        }

        addSyntax(gamemodearg) {
            val gamemode = context.get(gamemodearg)

            if (!sender.isPlayer) return@addSyntax

            val player = sender.asPlayer()

            player.sendActionBar(Component.text("Joining ${gamemode.value.gameName}...", NamedTextColor.GREEN))

            player.showTitle(
                Title.title(
                    Component.text("\uE00A"),
                    Component.empty(),
                    Title.Times.of(
                        Duration.ofMillis(500),
                        Duration.ofMillis(250),
                        Duration.ofMillis(500)
                    )
                )
            )

            Manager.scheduler.buildTask {
                sender.asPlayer().joinGameOrNew(gamemode.key, GameManager.registeredGameMap[gamemode.key]!!.defaultGameOptions)
            }.delay(Duration.ofMillis(500)).schedule()

        }
    }

}