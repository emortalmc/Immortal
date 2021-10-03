package emortal.immortal.commands

import emortal.immortal.game.GameManager
import emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import net.minestom.server.command.builder.arguments.ArgumentType
import world.cepi.kstom.Manager
import world.cepi.kstom.command.arguments.suggest
import world.cepi.kstom.command.kommand.Kommand
import java.time.Duration

object PlayCommand : Kommand({

    onlyPlayers

    val gamemodearg = ArgumentType.Word("gamemode").map { input: String ->
        GameManager.registeredGameMap.entries.firstOrNull { it.value.gameName == input && it.value.showsInPlayCommand }

    }.suggest {
        GameManager.registeredGameMap.values
            .filter { it.showsInPlayCommand }
            .map { it.gameName }
    }

    syntax(gamemodearg) {
        val gamemode = !gamemodearg

        player.sendActionBar(Component.text("Joining ${gamemode!!.value.gameName}...", NamedTextColor.GREEN))

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

}, "play")