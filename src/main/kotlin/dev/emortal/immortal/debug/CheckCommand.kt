package dev.emortal.immortal.debug

import dev.emortal.immortal.game.GameManager
import net.minestom.server.command.builder.Command
import world.cepi.kstom.Manager

internal object CheckCommand : Command("check") {

    init {
        setDefaultExecutor { sender, context ->
            val playerMapCheck = GameManager.playerGameMap.any { Manager.connection.getPlayer(it.key) == null }
            sender.sendMessage("map check: ${playerMapCheck}, size: ${GameManager.playerGameMap.size}")
            sender.sendMessage("registered game types: ${GameManager.registeredGameMap.size}")
            sender.sendMessage("registered game classes: ${GameManager.registeredClassMap.size}")

            val gameMapCheck = GameManager.gameMap.any { it.value.any { it.value.players.size == 0 && it.value.players.any { !it.isOnline } } }
            sender.sendMessage("Game map: ${gameMapCheck}")
            sender.sendMessage("Game map size: ${GameManager.gameMap.size}")
            sender.sendMessage("Game map size sum: ${GameManager.gameMap.values.sumOf { it.size }}")

            Manager.command.execute(sender, "gc")
        }
    }

}