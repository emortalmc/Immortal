package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.CampfireHandler
import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.ForceStartCommand
import dev.emortal.immortal.commands.PlayCommand
import dev.emortal.immortal.commands.SoundCommand
import dev.emortal.immortal.commands.SpectateCommand
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.LOGGER
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.network.packet.server.play.PlayerInfoPacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listen
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register
import java.time.Duration
import java.util.*

class ImmortalExtension : Extension() {

    companion object {
        val unregisterQueue = mutableListOf<UUID>()
        val lastInstanceMap = hashMapOf<Player, UUID>()
    }

    override fun initialize() {
        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.game?.removePlayer(player)
            player.game?.removeSpectator(player)
            GameManager.playerGameMap.remove(player)
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (isFirstSpawn) {
                player.isEnableRespawnScreen = false
            }

            LOGGER.info("${player.username} switched instance")
            if (unregisterQueue.contains(lastInstanceMap[player])) {
                val lastInstance = Manager.instance.getInstance(lastInstanceMap[player]!!)

                if (lastInstance?.players?.size == 0) {
                    Manager.instance.unregisterInstance(lastInstance)
                    LOGGER.info("Unregistered instance - Current instances: ${Manager.instance.instances.size}")
                }
            }

            lastInstanceMap[player] = spawnInstance.uniqueId

            /*if (player.game != null) {
                if (player.game?.instances?.contains(spawnInstance) == false) {
                    player.game?.removePlayer(player)
                    player.game?.removeSpectator(player)
                }
            }*/
        }

        eventNode.listenOnly<EntityTickEvent> {
            if (entity.position.y <= 0) {
                    entity.remove()
            }
            if (entity.chunk == null) {
                logger.warn("Entity was force removed because it was not in a chunk")
                if (entity is Player) {
                    (entity as Player).kick(Component.text("You were not in a chunk... somehow.\nCongratulations! You found a bug", NamedTextColor.RED))
                } else {
                    entity.remove()
                }
            }
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (player.gameMode == GameMode.ADVENTURE || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {
            if (player.gameMode == GameMode.ADVENTURE || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }

        val dimensionType = DimensionType.builder(NamespaceID.from("fullbright"))
            .ambientLight(1f)
            .build()
        Manager.dimensionType.addDimension(dimensionType)

        SignHandler.register("minecraft:sign")
        CampfireHandler.register("minecraft:campfire")
        SkullHandler.register("minecraft:skull")

        ForceStartCommand.register()
        PlayCommand.register()
        SoundCommand.register()
        SpectateCommand.register()

        logger.info("[Immortal] Initialized!")
    }

    override fun terminate() {
        ForceStartCommand.unregister()
        PlayCommand.unregister()
        SoundCommand.unregister()
        SpectateCommand.unregister()

        logger.info("[Immortal] Terminated!")
    }

}