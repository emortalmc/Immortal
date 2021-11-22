package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.CampfireHandler
import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.ForceStartCommand
import dev.emortal.immortal.commands.PlayCommand
import dev.emortal.immortal.commands.SpectateCommand
import dev.emortal.immortal.game.GameManager.game
import net.minestom.server.entity.GameMode
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.item.Material
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register

class ImmortalExtension : Extension() {

    override fun initialize() {
        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.game?.removePlayer(player)
            player.game?.removeSpectator(player)
        }
        eventNode.listenOnly<PlayerSpawnEvent> {
            if (player.game != null) {
                if (player.game!!.instance != spawnInstance) {
                    player.game!!.removePlayer(player)
                    player.game!!.removeSpectator(player)
                }
            }
        }

        eventNode.listenOnly<PlayerUseItemEvent> {
            if (player.game == null) return@listenOnly
            if (!player.game!!.spectators.contains(player)) return@listenOnly

            if (player.itemInMainHand.material == Material.COMPASS) {
                isCancelled = true


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
        SpectateCommand.register()

        logger.info("[Immortal] Initialized!")
    }

    override fun terminate() {
        ForceStartCommand.unregister()
        PlayCommand.unregister()
        SpectateCommand.unregister()

        logger.info("[Immortal] Terminated!")
    }

}