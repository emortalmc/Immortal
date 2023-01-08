package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.BannerHandler
import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameConfig
import dev.emortal.immortal.debug.ImmortalDebug
import dev.emortal.immortal.debug.SpamGamesCommand
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.terminal.ImmortalTerminal
import dev.emortal.immortal.util.JedisStorage
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.block.Block
import net.minestom.server.listener.UseEntityListener
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.litote.kmongo.serialization.SerializationClassMappingTypeService
import org.tinylog.kotlin.Logger
import java.nio.file.Path
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Immortal {

    lateinit var gameConfig: GameConfig
    private val configPath = Path.of("./config.json")

    val port get() = System.getProperty("port")?.toInt() ?: gameConfig.port
    val address get() = System.getProperty("address") ?: gameConfig.ip
    val redisAddress get() = System.getProperty("redisAddress") ?: gameConfig.redisAddress

    var terminalThread: Thread? = null

    fun init(eventNode: EventNode<Event> = MinecraftServer.getGlobalEventHandler()) {
        System.setProperty("org.litote.mongo.mapping.service", SerializationClassMappingTypeService::class.qualifiedName!!)

        gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig())

        // Ignore warning when player opens recipe book
        MinecraftServer.getPacketListenerManager().setListener(ClientSetRecipeBookStatePacket::class.java) { _: ClientSetRecipeBookStatePacket, _: Player -> }

        MinecraftServer.getPacketListenerManager().setListener(ClientInteractEntityPacket::class.java) { packet: ClientInteractEntityPacket, player: Player ->
            UseEntityListener.useEntityListener(packet, player)

            if (packet.type != ClientInteractEntityPacket.Interact(Player.Hand.MAIN) && packet.type != ClientInteractEntityPacket.Attack()) return@setListener

            PacketNPC.viewerMap[player.uuid]?.firstOrNull { it.playerId == packet.targetId }?.onClick(player)
        }

        if (redisAddress.isBlank()) {
            Logger.info("Running without Redis - Game to join: ${gameConfig.defaultGame}")

            if (gameConfig.defaultGame.isBlank()) {
                MinecraftServer.getSchedulerManager().buildTask {
                    Logger.error("Default game is blank in your config.json! Replace it or use Redis")
                    if (GameManager.getRegisteredNames().isNotEmpty()) {
                        Logger.error("Maybe try \"${GameManager.getRegisteredNames().first()}\"?")
                    }

                    exitProcess(1)
                }.delay(TaskSchedule.seconds(2)).schedule()

            }

            if (System.getProperty("debug").toBoolean()) {
                ImmortalDebug.enable()
            }
        } else {
            Logger.info("Running with Redis")
            JedisStorage.init()
        }

        ImmortalEvents.register(eventNode)

        val dimensionType = DimensionType.builder(NamespaceID.from("fullbright"))
            .ambientLight(2f)
            .build()
        MinecraftServer.getDimensionTypeManager().addDimension(dimensionType)

        val bm = MinecraftServer.getBlockManager()
        // For some reason minecraft:oak_wall_sign exists so yay
        // Required for TNT
        Block.values().forEach {
            if (it.name().endsWith("sign")) {
                bm.registerHandler(it.name()) { SignHandler }
            }
        }
        bm.registerHandler("minecraft:sign") { SignHandler }
        bm.registerHandler("minecraft:player_head") { SignHandler }
        bm.registerHandler("minecraft:skull") { SkullHandler }
        bm.registerHandler("minecraft:banner") { BannerHandler }

        val cm = MinecraftServer.getCommandManager()
        cm.register(ForceStartCommand)
        cm.register(ShortenStartCommand)
        cm.register(ForceGCCommand)
        cm.register(SoundCommand)
        cm.register(StatsCommand)
        cm.register(ListCommand)
        cm.register(SpamGamesCommand)
        cm.register(StopCommand)

        Logger.info("Immortal initialized!")
    }

    fun initAsServer() {
        gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig())

        val minestom = MinecraftServer.init()

        System.setProperty("minestom.entity-view-distance", gameConfig.entityViewDistance.toString())
        System.setProperty("minestom.chunk-view-distance", gameConfig.chunkViewDistance.toString())
        MinecraftServer.setCompressionThreshold(0)
        MinecraftServer.setBrandName("Minestom")
        MinecraftServer.setTerminalEnabled(false)

        if (gameConfig.velocitySecret.isBlank()) {
            if (gameConfig.onlineMode) MojangAuth.init()
        } else {
            VelocityProxy.enable(gameConfig.velocitySecret)
        }

        init()

        MinecraftServer.getSchedulerManager().buildShutdownTask {
            stop()
        }

        minestom.start(address, port)

        terminalThread = thread(start = true, isDaemon = true, name = "ImmortalConsole") {
            ImmortalTerminal.start()
        }
    }

    fun stop() {
//        ForceStartCommand.unregister()
//        ForceGCCommand.unregister()
//        SoundCommand.unregister()
//        StatsCommand.unregister()
//        ListCommand.unregister()
//        VersionCommand.unregister()

        GameManager.getRegisteredNames().forEach {
            JedisStorage.jedis?.publish("playercount", "$it 0")
        }

        JedisStorage.jedis?.close()

        Logger.info("Immortal terminated!")
    }

}