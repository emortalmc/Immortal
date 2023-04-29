package dev.emortal.immortal.debug

import dev.emortal.immortal.commands.StatsCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.timer.TaskSchedule
import kotlin.math.floor

object ImmortalDebug {

    fun enable() {
        val scheduler = MinecraftServer.getSchedulerManager()
        val instanceManager = MinecraftServer.getInstanceManager()
        val connectionManager = MinecraftServer.getConnectionManager()
        val benchmarkManager = MinecraftServer.getBenchmarkManager()

        val lastTick = StatsCommand.LAST_TICK

        scheduler.buildTask {
            connectionManager.onlinePlayers.forEach { player ->
                val ramUsage = benchmarkManager.usedMemory / 1024 / 1024
                val monitor = lastTick.get()
                val tickMs = floor(monitor.tickTime * 100) / 100

                val onlinePlayers = connectionManager.onlinePlayers.size
                val instances = instanceManager.instances
                val entities = instances.sumOf { it.entities.size } - onlinePlayers
                val chunks = instances.sumOf { it.chunks.size }

                player.sendPlayerListFooter(
                    Component.text()
                        .append(Component.text(ramUsage, NamedTextColor.GOLD))
                        .append(Component.text("MB", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(tickMs, NamedTextColor.GOLD))
                        .append(Component.text("mspt", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(onlinePlayers, NamedTextColor.GOLD))
                        .append(Component.text(" players", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(entities, NamedTextColor.GOLD))
                        .append(Component.text(" entities", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(instances.size, NamedTextColor.GOLD))
                        .append(Component.text(" instances", NamedTextColor.GOLD))
                        .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(chunks, NamedTextColor.GOLD))
                        .append(Component.text(" chunks", NamedTextColor.GOLD))
                )
            }
        }.repeat(TaskSchedule.tick(40)).schedule()
    }

}