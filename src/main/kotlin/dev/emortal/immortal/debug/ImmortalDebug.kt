package dev.emortal.immortal.debug

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.monitoring.TickMonitor
import net.minestom.server.timer.TaskSchedule
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.floor

object ImmortalDebug {

    fun enable() {
        val lastTick = AtomicReference<TickMonitor>()
        Manager.globalEvent.listenOnly<ServerTickMonitorEvent> {
            lastTick.set(tickMonitor)
        }

        Manager.scheduler.buildTask {
            Manager.connection.onlinePlayers.forEach { player ->
                val ramUsage = Manager.benchmark.usedMemory / 1024 / 1024
                val monitor = lastTick.get()
                val tickMs = floor(monitor.tickTime * 100) / 100

                val onlinePlayers = Manager.connection.onlinePlayers.size
                val instances = Manager.instance.instances
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