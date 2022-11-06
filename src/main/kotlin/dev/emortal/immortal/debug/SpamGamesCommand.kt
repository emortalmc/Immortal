package dev.emortal.immortal.debug

import dev.emortal.immortal.game.GameManager
import net.minestom.server.MinecraftServer
import net.minestom.server.command.ConsoleSender
import net.minestom.server.command.builder.Command
import net.minestom.server.timer.TaskSchedule


internal object SpamGamesCommand : Command("spamgames") {

    init {
        setCondition { sender, _ ->
            sender is ConsoleSender
        }
        setDefaultExecutor { sender, _ ->
            if (sender !is ConsoleSender) return@setDefaultExecutor
            sender.sendMessage("spamming games")

            var i = 0;
            MinecraftServer.getSchedulerManager().submitTask {
                repeat(40) {
                    var game = GameManager.createGame("marathon")

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