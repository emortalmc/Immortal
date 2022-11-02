package dev.emortal.immortal.debug

import dev.emortal.immortal.game.GameManager
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.timer.TaskSchedule


internal object SpamGamesCommand : Command("spamgames") {

    init {
        setDefaultExecutor { sender, _ ->
            sender.sendMessage("spamming games")

            var i = 0;
            MinecraftServer.getSchedulerManager().submitTask {
                repeat(40) {
                    var game = GameManager.createGame("lazertag")

                    game?.thenAccept {
                        it.end()
                    }

                    game = null
                }
                i++
                return@submitTask TaskSchedule.nextTick()

            }
        }
    }

}