package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameConfig
import dev.emortal.immortal.debug.DebugSpectateCommand
import dev.emortal.immortal.debug.ImmortalDebug
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.luckperms.LuckpermsListener
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.RedisStorage
import dev.emortal.immortal.util.RedisStorage.unregisterTopic
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.extensions.Extension
import net.minestom.server.instance.block.Block
import net.minestom.server.listener.UseEntityListener
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.util.register
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class ImmortalExtension : Extension() {

    companion object {
        lateinit var luckperms: LuckPerms

        lateinit var gameConfig: GameConfig
        private val configPath = Path.of("./immortalconfig.json")

        fun init(eventNode: EventNode<Event> = Manager.globalEvent) {
            gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig("replaceme", 42069))
            luckperms = LuckPermsProvider.get()

            // Ignore warning when player opens recipe book
            Manager.packetListener.setListener(ClientSetRecipeBookStatePacket::class.java) { _: ClientSetRecipeBookStatePacket, _: Player -> }

            Manager.packetListener.setListener(ClientInteractEntityPacket::class.java) { packet: ClientInteractEntityPacket, player: Player ->
                UseEntityListener.useEntityListener(packet, player)

                if (packet.type != ClientInteractEntityPacket.Interact(Player.Hand.MAIN) && packet.type != ClientInteractEntityPacket.Attack()) return@setListener

                PacketNPC.viewerMap[player.uuid]?.firstOrNull { it.playerId == packet.targetId }?.onClick(player)
            }

            val debugMode = System.getProperty("debug").toBoolean()
            val debugGame = System.getProperty("debuggame")
            if (debugMode) {
                ImmortalDebug.enable(debugGame)
                DebugSpectateCommand.register()
            }

            ImmortalEvents.register(eventNode)
            RedisStorage.init()

            val dimensionType = DimensionType.builder(NamespaceID.from("fullbright"))
                .ambientLight(2f)
                .build()
            Manager.dimensionType.addDimension(dimensionType)

            // For some reason minecraft:oak_wall_sign exists so yay
            Block.values().forEach {
                if (it.name().endsWith("sign")) {
                    SignHandler.register(it.name())
                }
            }
            SignHandler.register("minecraft:sign")
            SkullHandler.register("minecraft:skull")

            ForceStartCommand.register()
            ForceGCCommand.register()
            SoundCommand.register()
            StatsCommand.register()
            ListCommand.register()
            VersionCommand.register()
            SettingsCommand.register()

            Logger.info("Immortal initialized!")
        }
    }

    override fun initialize() {
        init(eventNode)

        LuckpermsListener(this@ImmortalExtension, luckperms)
    }

    override fun terminate() {
        ForceStartCommand.unregister()
        ForceGCCommand.unregister()
        SoundCommand.unregister()
        StatsCommand.unregister()
        ListCommand.unregister()
        VersionCommand.unregister()
        SettingsCommand.unregister()
        DebugSpectateCommand.unregister()

        GameManager.registeredGameMap.keys().asIterator().forEachRemaining {
            unregisterTopic?.publishAsync(it)
            Logger.info("Unregistering game ${it}")
        }

        RedisStorage.redisson?.shutdown(0, 3, TimeUnit.SECONDS)


        Logger.info("Immortal terminated!")
    }

}