package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameListing
import dev.emortal.immortal.config.GameListingConfig
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.LuckpermsListener
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.luckperms.PermissionUtils.lpUser
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.resetTeam
import kotlinx.coroutines.NonCancellable.isCancelled
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.adventure.audience.Audiences.players
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register
import java.nio.file.Path
import java.time.Duration

class ImmortalExtension : Extension() {

    companion object {
        lateinit var luckperms: LuckPerms

        lateinit var gameListingConfig: GameListingConfig
        val gameListingPath = Path.of("./gameListings.json")
    }

    override fun initialize() {
        luckperms = LuckPermsProvider.get()
        LuckpermsListener(this, luckperms)

        gameListingConfig = ConfigHelper.initConfigFile(gameListingPath, GameListingConfig())

        GameManager.registeredGameMap.forEach {
            if (!gameListingConfig.gameListings.contains(it.value.gameName)) {
                gameListingConfig.gameListings[it.value.gameName] = GameListing()
            }
        }

        ConfigHelper.writeObjectToPath(gameListingPath, gameListingConfig)

        val instanceManager = Manager.instance

        eventNode.listenOnly<PlayerChunkUnloadEvent> {
            val chunk = instance.getChunk(chunkX, chunkZ) ?: return@listenOnly

            if (chunk.viewers.isEmpty()) {
                try {
                    instance.unloadChunk(chunkX, chunkZ)
                } catch (_: NullPointerException) {
                }
            }
        }

        val globalEvent = Manager.globalEvent

        val maxDistanceSurvival = 10*10
        val maxDistanceCreative = 50*50
        val maxDistanceSpectator = 100*100
        globalEvent.listenOnly<PlayerMoveEvent> {
            val distanceSquared = player.position.distanceSquared(newPosition)

            val aboveMax = when (player.gameMode) {
                GameMode.SURVIVAL, GameMode.ADVENTURE -> distanceSquared > maxDistanceSurvival
                GameMode.SPECTATOR -> distanceSquared > maxDistanceSpectator
                GameMode.CREATIVE -> distanceSquared > maxDistanceCreative
                else -> false
            }

            if (aboveMax) isCancelled = true
        }

        val cooldown = Duration.ofMinutes(20)
        Manager.scheduler.buildTask {
            var leftoverInstances = 0
            instanceManager.instances.forEach {
                if (it.players.isEmpty()) {
                    if (it.hasTag(GameManager.doNotUnregisterTag)) return@buildTask

                    instanceManager.unregisterInstance(it)
                    leftoverInstances++
                }
            }
            if (leftoverInstances != 0) logger.warn("$leftoverInstances empty instances were found and unregistered.")
        }.delay(cooldown).repeat(cooldown).schedule()

        eventNode.listenOnly<RemoveEntityFromInstanceEvent> {
            if (this.entity !is Player) return@listenOnly
            logger.info("Player removed!")

            this.instance.scheduleNextTick {
                if (it.players.isEmpty()) {
                    if (it.hasTag(GameManager.doNotUnregisterTag)) return@scheduleNextTick

                    instanceManager.unregisterInstance(it)
                    logger.info("Instance was unregistered. Instance count: ${instanceManager.instances.size}")
                } else {
                    logger.warn("Couldn't unregister instance, players: ${it.players.joinToString(separator = ", ") { it.username }}")
                }
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.game?.removePlayer(player)
            player.game?.removeSpectator(player)
            GameManager.playerGameMap.remove(player)
            PermissionUtils.userToPlayerMap.remove(player.lpUser)
        }

        eventNode.listenOnly<PlayerPacketEvent> {
            if (packet is ClientInteractEntityPacket) {
                val packet = packet as ClientInteractEntityPacket
                if (packet.type != ClientInteractEntityPacket.Interact(Player.Hand.MAIN) && packet.type != ClientInteractEntityPacket.Attack()) return@listenOnly
                PacketNPC.npcIdMap[packet.targetId]?.onClick(player)
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            logger.info("PLAYER ${player.username} JUST SPAWNED!!")
            if (this.isFirstSpawn) {
                player.resetTeam()
                player.setCustomSynchronizationCooldown(Duration.ofSeconds(20))

                if (player.hasLuckPermission("*")) {
                    player.permissionLevel = 4
                }

                PermissionUtils.refreshPrefix(player)
            } else {
                val viewingNpcs = (PacketNPC.viewerMap[player.uuid] ?: return@listenOnly).toMutableList()
                viewingNpcs.forEach {
                    it.removeViewer(player)
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
            .ambientLight(2f)
            .build()
        Manager.dimensionType.addDimension(dimensionType)

        SignHandler.register("minecraft:sign")
        SkullHandler.register("minecraft:skull")

        ForceStartCommand.register()
        SpectateCommand.register()
        SoundCommand.register()
        StatsCommand.register()
        PlayCommand.register()
        ListCommand.register()
        GCCommand.register()

        logger.info("[${origin.name}] Initialized!")
    }

    override fun terminate() {
        ForceStartCommand.unregister()
        SpectateCommand.unregister()
        SoundCommand.unregister()
        StatsCommand.unregister()
        PlayCommand.unregister()
        ListCommand.unregister()
        GCCommand.unregister()

        logger.info("[${origin.name}] Terminated!")
    }

}