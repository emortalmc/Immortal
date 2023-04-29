package dev.emortal.immortal.commands

import com.sun.management.GarbageCollectorMXBean
import com.sun.management.GcInfo
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.util.armify
import dev.emortal.immortal.util.parsed
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.command.builder.Command
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.instance.SharedInstance
import net.minestom.server.monitoring.TickMonitor
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import javax.management.Attribute
import javax.management.AttributeList
import javax.management.ObjectName
import kotlin.math.floor


internal object StatsCommand : Command("tps") {

    public val LAST_TICK = AtomicReference<TickMonitor>()
    private val EXCLUDED_MEMORY_SPACES: List<Pattern> = setOf("Metaspace", "Compressed Class Space", "^CodeHeap").map { Pattern.compile(it) }

    init {

        MinecraftServer.getGlobalEventHandler().addListener(ServerTickMonitorEvent::class.java) { e ->
            LAST_TICK.set(e.tickMonitor)
        }
    }

    init {

        val connectionManager = MinecraftServer.getConnectionManager()
        val instanceManager = MinecraftServer.getInstanceManager()

        setDefaultExecutor { sender, _ ->
            val totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024
            val freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024
            val ramUsage = totalMem - freeMem

            val mbs = ManagementFactory.getPlatformMBeanServer()
            val name = ObjectName.getInstance("java.lang:type=OperatingSystem")
            val list: AttributeList = mbs.getAttributes(name, arrayOf("ProcessCpuLoad"))
            val att = list.firstOrNull() as? Attribute
            val value = (att?.value as? Double) ?: 0.0
            val cpuPercent = floor(value * 100_00) / 100.0

            val monitor = LAST_TICK.get()
            val tickMs = monitor.tickTime
            val tpsSetting = MinecraftServer.TICK_PER_SECOND
            val maxTickMs = MinecraftServer.TICK_MS
            val tps = floor(1000 / tickMs).toInt().coerceAtMost(tpsSetting)

            val onlinePlayers = connectionManager.onlinePlayers.size
            val entities = instanceManager.instances.sumOf { it.entities.size } - onlinePlayers
            val instances = instanceManager.instances
            val sharedInstances = instances.count { it is SharedInstance }
            val permanentInstances = instances.count { it.hasTag(GameManager.doNotUnregisterTag) }
            val chunks = instanceManager.instances.sumOf { it.chunks.size }

            sender.sendMessage(
                Component.textOfChildren(
                    // RAM details
                    Component.text("RAM: ", NamedTextColor.GRAY),
                    Component.text("${ramUsage}MB / ${totalMem}MB", NamedTextColor.GRAY),
                    Component.text(" (", NamedTextColor.GRAY),
                    Component.text("${floor((ramUsage.toDouble() / totalMem.toDouble()) * 100.0).toInt()}%", NamedTextColor.GREEN),
                    Component.text(")", NamedTextColor.GRAY),

                    // CPU details
                    Component.text("\nCPU: ", NamedTextColor.GRAY),
                    Component.text("${if (cpuPercent < 0) "..." else cpuPercent}%", NamedTextColor.GREEN),

                    // TPS details
                    Component.text("\nTPS: ", NamedTextColor.GRAY),
                    Component.text(tps, NamedTextColor.GREEN),
                    Component.text(" (", NamedTextColor.GRAY),
                    Component.text("${floor(tickMs * 1000.0) / 1000}ms", TextColor.lerp(tickMs.toFloat() / maxTickMs.toFloat(), NamedTextColor.GREEN, NamedTextColor.RED)),
                    Component.text(")\n", NamedTextColor.GRAY),

                    createGcComponent(),

                    // Entities
                    Component.text("\nEntities: ", NamedTextColor.GRAY)
                        .append(Component.text(entities + onlinePlayers, NamedTextColor.GOLD))
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.text()
                                    .append(Component.text("Players: ", NamedTextColor.GRAY))
                                    .append(Component.text(onlinePlayers, NamedTextColor.GOLD))
                                    .append(Component.text("\nEntities: ", NamedTextColor.GRAY))
                                    .append(Component.text(entities, NamedTextColor.GOLD))
                                    .build()
                            )),

                    // Instances
                    Component.text("\nInstances: ", NamedTextColor.GRAY)
                        .append(Component.text(instances.size, NamedTextColor.GOLD))
                        .hoverEvent(HoverEvent.showText(
                            // hover stuffs :)
                            Component.text()
                                .append(Component.text("Shared: ", NamedTextColor.GRAY))
                                .append(Component.text(sharedInstances, NamedTextColor.GOLD))
                                .append(Component.text("\nPermanent: ", NamedTextColor.GRAY))
                                .append(Component.text(permanentInstances, NamedTextColor.GOLD))
                                .append(Component.text("\nChunks: ", NamedTextColor.GRAY))
                                .append(Component.text(chunks, NamedTextColor.GOLD))
                                .build()
                        )),

                    // Games
                    Component.text("\nGames: ", NamedTextColor.GRAY)
                        .append(Component.text(GameManager.getRegisteredNames().map { GameManager.getGames(it) }.sumOf { it?.size ?: 0 }, NamedTextColor.GOLD))
                        .hoverEvent(HoverEvent.showText(
                            // hover stuffs :)
                            Component.text().also {
                                GameManager.getRegisteredNames().forEach { gameName ->
                                    it.append(
                                        Component.text()
                                            .append(Component.text(gameName, NamedTextColor.YELLOW))
                                            .append(Component.text("\n  Games: ", NamedTextColor.GRAY))
                                            .append(Component.text(GameManager.getGames(gameName)!!.size, NamedTextColor.GOLD))
                                    )
                                }
                            }.build()
                        ))

                )
                    .armify(40)
            )
        }

    }

    private fun createGcComponent(): Component {
        val builder = Component.text()
            .append(Component.text("GC Info:", NamedTextColor.GRAY))

        this.getGcInfo().entries.forEach { entry ->
            val entryBuilder = Component.text()

            val millisSinceRun: Long = getUptime() - entry.value.endTime
            val lastRunText = (millisSinceRun / 1000).parsed()


            entryBuilder.append(Component.text("\n  " + entry.key + ":", NamedTextColor.GRAY))
                .append(Component.text("\n    Last Run: ", NamedTextColor.GRAY))
                .append(Component.text(lastRunText, NamedTextColor.GOLD));

            entryBuilder.hoverEvent(HoverEvent.showText(this.createGcHover(entry.key, entry.value)))

            builder.append(entryBuilder)
        }

        return builder.build()
    }

    private fun createGcHover(name: String, info: GcInfo): Component {
        val builder = Component.text()
            .append(Component.text("Name: ", NamedTextColor.GOLD))
            .append(Component.text(name, NamedTextColor.GRAY))
            .append(Component.newline())
            .append(Component.text("Duration: ", NamedTextColor.GOLD))
            .append(Component.text(info.duration.toString() + "ms", NamedTextColor.GRAY))
            .append(Component.newline(), Component.newline())
            .append(Component.text("Memory After:", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(createMemoryUsagePeriod(info.memoryUsageAfterGc))
            .append(Component.newline(), Component.newline())
            .append(Component.text("Memory Before:", NamedTextColor.GOLD))
            .append(Component.newline())
            .append(createMemoryUsagePeriod(info.memoryUsageBeforeGc))
        return builder.build()
    }


    private fun createMemoryUsagePeriod(memoryUsageMap: Map<String, MemoryUsage>): Component {
        val lines: MutableList<Component> = ArrayList()

        memoryUsageMap.entries.forEach { entry ->
            if (EXCLUDED_MEMORY_SPACES.any { it.matcher(entry.key).find() }) return@forEach

            lines.add(Component.text().append(Component.text("  " + entry.key + ": ", NamedTextColor.GOLD))
                .append(Component.text("${entry.value.used / 1024 / 1024} MB", NamedTextColor.GRAY))
                .build());
        }

        return Component.join(JoinConfiguration.newlines(), lines)
    }

    private fun getGcInfo(): Map<String, GcInfo> {
        val gcInfo: MutableMap<String, GcInfo> = HashMap()
        for (garbageCollectorMXBean in ManagementFactory.getGarbageCollectorMXBeans()) {
            val bean = garbageCollectorMXBean as GarbageCollectorMXBean
            if (bean.lastGcInfo == null) continue
            gcInfo[bean.name] = bean.lastGcInfo
        }
        return gcInfo
    }

    private fun getUptime(): Long {
        return ManagementFactory.getRuntimeMXBean().uptime
    }



}