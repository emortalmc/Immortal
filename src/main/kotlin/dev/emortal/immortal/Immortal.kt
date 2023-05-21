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
import dev.emortal.immortal.util.JedisStorage
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.extras.MojangAuth
import net.minestom.server.extras.velocity.VelocityProxy
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.client.play.ClientChatSessionUpdatePacket
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.litote.kmongo.serialization.SerializationClassMappingTypeService
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.system.exitProcess

object Immortal {

    private val LOGGER = LoggerFactory.getLogger(Immortal::class.java)

    lateinit var gameConfig: GameConfig
    private val configPath = Path.of("./config.json")

    val port get() = System.getProperty("port")?.toInt() ?: gameConfig.port
    val address get() = System.getProperty("address") ?: gameConfig.ip
    val redisAddress get() = System.getProperty("redisAddress") ?: gameConfig.redisAddress

    fun init(registerEvents: Boolean = true, eventNode: EventNode<Event> = MinecraftServer.getGlobalEventHandler()) {
        System.setProperty("org.litote.mongo.mapping.service", SerializationClassMappingTypeService::class.qualifiedName!!)

        gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig())

        // Ignore warning when player opens recipe book
        MinecraftServer.getPacketListenerManager().setListener(ClientSetRecipeBookStatePacket::class.java) { _: ClientSetRecipeBookStatePacket, _: Player -> }
        MinecraftServer.getPacketListenerManager().setListener(ClientChatSessionUpdatePacket::class.java) { _: ClientChatSessionUpdatePacket, _: Player -> }

        if (redisAddress.isBlank()) {
            LOGGER.info("Running without Redis - Game to join: ${gameConfig.defaultGame}")

            if (gameConfig.defaultGame.isBlank()) {
                MinecraftServer.getSchedulerManager().buildTask {
                    LOGGER.error("Default game is blank in your config.json! Replace it or use Redis")
                    if (GameManager.getRegisteredNames().isNotEmpty()) {
                        LOGGER.error("Maybe try \"${GameManager.getRegisteredNames().first()}\"?")
                    }

                    exitProcess(1)
                }.delay(TaskSchedule.seconds(2)).schedule()

            }

            if (System.getProperty("debug").toBoolean()) {
                ImmortalDebug.enable()
            }
        } else {
            LOGGER.info("Running with Redis")
            JedisStorage.init()
        }

        if (registerEvents) ImmortalEvents.register(eventNode)

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

        LOGGER.info("Immortal initialized!")
    }

    fun initAsServer(registerEvents: Boolean = true, eventNode: EventNode<Event>? = null) {
        gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig())

        System.setProperty("minestom.entity-view-distance", gameConfig.entityViewDistance.toString())
        System.setProperty("minestom.chunk-view-distance", gameConfig.chunkViewDistance.toString())

        val minestom = MinecraftServer.init()
        MinecraftServer.setCompressionThreshold(0)

        if (gameConfig.velocitySecret.isBlank()) {
            if (gameConfig.onlineMode) MojangAuth.init()
        } else {
            VelocityProxy.enable(gameConfig.velocitySecret)
        }

        init(registerEvents, eventNode ?: MinecraftServer.getGlobalEventHandler())

        minestom.start(address, port)
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

        LOGGER.info("Immortal terminated!")
    }

}