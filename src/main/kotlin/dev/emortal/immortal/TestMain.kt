package dev.emortal.immortal

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.TestingGame
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule
import org.tinylog.kotlin.Logger

fun main() {
    Logger.info("Test main starting...")
    Immortal.initAsServer()

    MinecraftServer.getSchedulerManager().buildTask {
        Logger.info("1 second!")
    }.delay(TaskSchedule.seconds(1)).schedule()

    GameManager.registerGame<TestingGame>(
        "testinggame",
        Component.text("Testing Game"),
        showsInSlashPlay = true
    )
    Logger.info("Test main started")
}