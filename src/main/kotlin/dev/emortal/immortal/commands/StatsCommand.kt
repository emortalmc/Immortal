package dev.emortal.immortal.commands

import dev.emortal.immortal.commands.StatsCommand.LAST_TICK
import dev.emortal.immortal.util.armify
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.instance.SharedInstance
import net.minestom.server.monitoring.TickMonitor
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand
import world.cepi.kstom.event.listenOnly
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.floor


object StatsCommand : Kommand({

    default {
        val ramUsage = Manager.benchmark.usedMemory / 1024 / 1024
        val monitor = LAST_TICK.get()
        val tickMs = monitor.tickTime
        val tps = floor(1000 / tickMs).toInt().coerceAtMost(20)

        val onlinePlayers = Manager.connection.onlinePlayers.size
        val entities = Manager.instance.instances.sumOf { it.entities.size } - onlinePlayers
        val instances = Manager.instance.instances
        val sharedInstances = instances.count { it is SharedInstance }
        val chunks = Manager.instance.instances.sumOf { it.chunks.size }

        sender.sendMessage(
            Component.text()
                .append(Component.text("RAM Usage: ", NamedTextColor.GRAY))
                .append(Component.text("${ramUsage}MB", NamedTextColor.GOLD))

                .append(Component.text("\nTPS: ", NamedTextColor.GRAY))
                .append(Component.text("$tps", NamedTextColor.GOLD))
                .append(Component.text(" (Last tick: ${tickMs})", NamedTextColor.DARK_GRAY))

                .append(Component.text("\nPlayers: ", NamedTextColor.GRAY))
                .append(Component.text(onlinePlayers, NamedTextColor.GOLD))

                .append(Component.text("\nEntities: ", NamedTextColor.GRAY))
                .append(Component.text(entities, NamedTextColor.GOLD))

                .append(Component.text("\nInstances: ", NamedTextColor.GRAY))
                .append(Component.text(instances.size - sharedInstances, NamedTextColor.GOLD))
                .append(Component.text(", Shared Instances: ", NamedTextColor.GRAY))
                .append(Component.text(sharedInstances, NamedTextColor.GOLD))

                .append(Component.text("\nLoaded Chunks: ", NamedTextColor.GRAY))
                .append(Component.text(chunks, NamedTextColor.GOLD))

                .armify(50)
        )
    }

}, "perf", "performance", "tps") {

    private val LAST_TICK = AtomicReference<TickMonitor>()

    init {
        Manager.globalEvent.listenOnly<ServerTickMonitorEvent> {
            LAST_TICK.set(tickMonitor)
        }
    }

}