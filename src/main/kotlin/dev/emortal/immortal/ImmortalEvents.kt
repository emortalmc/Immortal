package dev.emortal.immortal

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.util.JedisStorage
import dev.emortal.immortal.util.resetTeam
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.*
import net.minestom.server.utils.chunk.ChunkUtils
import org.slf4j.LoggerFactory
import java.util.*

object ImmortalEvents {

    private val LOGGER = LoggerFactory.getLogger(ImmortalEvents::class.java)

    fun register(eventNode: EventNode<Event>) {
        LOGGER.info("Registering events!")

        if (Immortal.redisAddress.isBlank()) eventNode.addListener(PlayerChatEvent::class.java) { e ->
            e.isCancelled = true
        }

        eventNode.addListener(PlayerLoginEvent::class.java) { e ->
            val player = e.player

            var subgame = Immortal.gameConfig.defaultGame.ifBlank {
                // Read then delete value
                JedisStorage.jedis?.getDel("${player.uuid}-subgame")?.trim()
            }

            if (subgame == null && GameManager.getRegisteredNames().size == 1) {
                subgame = GameManager.getRegisteredNames().first()
            }

            if (subgame == null) {
                LOGGER.error("Subgame is null!")
                player.kick("Game was not provided. Try /play")
                return@addListener
            }

            LOGGER.warn(subgame)

            val args = subgame.split(" ")
            val isSpectating = args.size > 1 && args[1].toBoolean()

            if (!isSpectating && !GameManager.getRegisteredNames().contains(args[0])) {
                LOGGER.error("${args[0]} game is not on this server")
                player.kick("${args[0]} game is not on this server. Try /play")
                return@addListener
            }

            if (isSpectating) {
                val playerToSpectate = MinecraftServer.getConnectionManager().getPlayer(UUID.fromString(args[2]))
                if (playerToSpectate == null) {
                    LOGGER.warn("Player to spectate was null")
                    player.kick("That player is not online")
                    return@addListener
                }
                val game = playerToSpectate.game
                if (game == null) {
                    player.kick("That player is not in a game")
                    return@addListener
                }

                player.setTag(GameManager.playerSpectatingTag, true)
                player.respawnPoint = game.getSpawnPosition(player, spectator = true)

                e.setSpawningInstance(game.instance!!)
            } else {
                val newGameFuture = GameManager.findOrCreateGame(player, args[0])

                val newGame = newGameFuture.join()
                newGame.playerCount.incrementAndGet()

//                Logger.info("player ${player.username} joining game ${newGame.id} with ${newGame.players.size} players")
//                Logger.info("game is joinable? ${newGame.canBeJoined(player)}")

                val respawnPoint = newGame.getSpawnPosition(player, spectator = false)
                player.respawnPoint = respawnPoint
                e.setSpawningInstance(newGame.instance!!)
            }
        }

        @Suppress("UnstableApiUsage")
        eventNode.addListener(PlayerChunkUnloadEvent::class.java) { e ->
            val instance = e.instance

            if (instance.hasTag(GameManager.doNotAutoUnloadChunkTag)) return@addListener

            val index = instance.getTag(GameManager.doNotUnloadChunksIndex)
            if (index != null) {
                val spawnX = ChunkUtils.getChunkCoordX(index)
                val spawnZ = ChunkUtils.getChunkCoordZ(index)
                val radius = instance.getTag(GameManager.doNotUnloadChunksRadius) ?: 2
                if ( // If chunk is in radius...
                    !(e.chunkX > spawnX + radius
                            && e.chunkX < spawnX - radius
                            && e.chunkZ > spawnZ + radius
                            && e.chunkZ < spawnZ - radius)
                ) {
                    // ...do not unload it
                    return@addListener
                }
            }

            val chunk = instance.getChunk(e.chunkX, e.chunkZ) ?: return@addListener

            if (chunk.viewers.isEmpty()) {
                instance.unloadChunk(e.chunkX, e.chunkZ)
            }
        }

        val globalEvent = MinecraftServer.getGlobalEventHandler()

        globalEvent.addListener(PlayerMoveEvent::class.java) { e ->
            // Teleport before player dies from void
            if (e.player.position.y < -20) {
                e.player.teleport(e.player.respawnPoint)
            }
        }

        eventNode.addListener(PlayerBlockPlaceEvent::class.java) { e ->
            if (e.instance.getBlock(e.blockPosition).isSolid) {
                e.isCancelled = true
            }
        }

        eventNode.addListener(PlayerDisconnectEvent::class.java) { e ->
            e.player.removeTag(GameManager.joiningGameTag)
        }

        eventNode.addListener(PlayerStartSneakingEvent::class.java) { e ->
            if (e.player.gameMode == GameMode.SPECTATOR) {
                e.player.stopSpectating()
            }
        }

        eventNode.addListener(PlayerSpawnEvent::class.java) { e ->
            if (e.isFirstSpawn) {
                e.player.resetTeam()

                if (e.player.hasLuckPermission("*")) {
                    e.player.permissionLevel = 4
                }

                PermissionUtils.refreshPrefix(e.player)
            }
        }
    }

}