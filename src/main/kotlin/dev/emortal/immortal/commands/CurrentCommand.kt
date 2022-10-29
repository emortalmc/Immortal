package dev.emortal.immortal.commands

import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.util.armify
import net.kyori.adventure.text.Component
import net.minestom.server.command.builder.Command
import net.minestom.server.entity.Player


internal object CurrentCommand : Command("current") {

    init {

        setDefaultExecutor { sender, _ ->

            val player = sender as? Player ?: return@setDefaultExecutor
            val playerGame = player.game

            sender.sendMessage(
                Component.textOfChildren(
                    Component.text("Current game: "),
                    Component.text("${playerGame?.gameName} ${playerGame?.id}"),
                    Component.text("\nCurrent instance: "),
                    Component.text("${player.instance?.uniqueId}")
                ).armify(40)
            )
        }

    }

}