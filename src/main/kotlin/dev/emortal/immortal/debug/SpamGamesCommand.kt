package dev.emortal.immortal.debug

import dev.emortal.immortal.game.GameManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minestom.server.MinecraftServer
import net.minestom.server.command.ConsoleSender
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.ArgumentString
import net.minestom.server.timer.TaskSchedule


internal object SpamGamesCommand : Command("spamgames") {

    init {
        setCondition { sender, _ ->
            sender is ConsoleSender
        }

        val gamemodeArgument = ArgumentString("gamemode")

        addConditionalSyntax({ sender, _ ->
            sender is ConsoleSender
        }, { sender, context ->
            if (sender !is ConsoleSender) return@addConditionalSyntax

            val gameMode = context.get(gamemodeArgument)


            runBlocking {
                launch {
                    var i = 0
                    MinecraftServer.getSchedulerManager().submitTask {
                        repeat(4) {
                            launch {
                                var game = GameManager.createGame(gameMode)

                                game!!.thenAccept {
                                    it.end()
                                }

                                game = null
                            }
                        }
                        i++
                        return@submitTask TaskSchedule.nextTick()

                    }
                }
            }

        }, gamemodeArgument)
    }

}