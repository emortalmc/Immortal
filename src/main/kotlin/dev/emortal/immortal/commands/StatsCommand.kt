package dev.emortal.immortal.commands

import dev.emortal.immortal.commands.StatsCommand.LAST_TICK
import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.progressBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.instance.SharedInstance
import net.minestom.server.monitoring.TickMonitor
import world.cepi.kstom.Manager
import world.cepi.kstom.command.kommand.Kommand
import world.cepi.kstom.event.listenOnly
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicReference
import javax.management.Attribute
import javax.management.AttributeList
import javax.management.ObjectName
import kotlin.math.floor


object StatsCommand : Kommand({

    default {
        val totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024
        val freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024
        val ramUsage = totalMem - freeMem
        val ramPercent = ramUsage.toFloat() / totalMem.toFloat()

        val mbs = ManagementFactory.getPlatformMBeanServer()
        val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
        val list: AttributeList = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))
        val att = list.firstOrNull() as? Attribute
        val value = (att?.value as? Double) ?: 0.0
        val cpuPercent = floor(value * 100_00) / 100.0

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
                .append(Component.text("RAM Usage: \n", NamedTextColor.GRAY))
                .append(progressBar(ramPercent, 30, "┃", NamedTextColor.GREEN, TextColor.color(0, 123, 0)))
                .append(Component.text(" ${ramUsage}MB / ${totalMem}MB\n", NamedTextColor.GRAY))

                .append(Component.text("\nCPU Usage: ", NamedTextColor.GRAY))
                .append(Component.text("${cpuPercent}%", NamedTextColor.GREEN))

                .append(Component.text("\nTPS: ", NamedTextColor.GRAY))
                .append(Component.text(tps, NamedTextColor.GREEN))

                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))

                .append(Component.text("MSPT: ", NamedTextColor.GRAY))
                .append(Component.text("${floor(tickMs * 100) / 100}ms\n", NamedTextColor.GREEN))

                .append(Component.text("\nPlayers: ", NamedTextColor.GRAY))
                .append(Component.text(onlinePlayers, NamedTextColor.GOLD))

                .append(Component.text(" | ", NamedTextColor.DARK_GRAY))

                .append(Component.text("Entities: ", NamedTextColor.GRAY))
                .append(Component.text(entities, NamedTextColor.GOLD))

                .append(Component.text("\nInstances: ", NamedTextColor.GRAY))
                .append(Component.text(instances.size - sharedInstances, NamedTextColor.GOLD))
                .append(Component.text(" (Shared: ${sharedInstances})", NamedTextColor.GRAY))

                .append(Component.text("\nActive Chunks: ", NamedTextColor.GRAY))
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