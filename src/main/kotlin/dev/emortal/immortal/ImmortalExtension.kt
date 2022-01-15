package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.CampfireHandler
import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.entity.EntityTickEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
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
            GameManager.playerGameMap.remove(player)

            if (instance.players.size == 0) {
                Manager.instance.unregisterInstance(instance)
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {

            /*if (player.game != null) {
                if (player.game?.instances?.contains(spawnInstance) == false) {
                    player.game?.removePlayer(player)
                    player.game?.removeSpectator(player)
                }
            }*/
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

        //ForceStartCommand.register()
        PlayCommand.register()
        //SoundCommand.register()
        SpectateCommand.register()

        //InstanceCommand.register()

        logger.info("[Immortal] Initialized!")
    }

    override fun terminate() {
        //ForceStartCommand.unregister()
        PlayCommand.unregister()
        //SoundCommand.unregister()
        SpectateCommand.unregister()

        //InstanceCommand.unregister()

        logger.info("[Immortal] Terminated!")
    }

}