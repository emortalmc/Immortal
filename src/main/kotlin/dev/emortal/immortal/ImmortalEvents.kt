package dev.emortal.immortal

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.JedisStorage
import dev.emortal.immortal.util.resetTeam
import net.minestom.server.entity.GameMode
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.*
import net.minestom.server.utils.chunk.ChunkUtils
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.util.*

object ImmortalEvents {

    fun register(eventNode: EventNode<Event>) {
        if (!System.getProperty("debug").toBoolean()) eventNode.listenOnly<PlayerChatEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            var subgame = Immortal.gameConfig.defaultGame.ifBlank {
                // Read then delete value
                JedisStorage.jedis?.getDel("${player.uuid}-subgame")?.trim()
            }

            if (subgame == null && GameManager.getRegisteredNames().size == 1) {
                subgame = GameManager.getRegisteredNames().first()
            }

            if (subgame == null) {
                Logger.error("Subgame is null!")
                player.kick("Game was not provided. Try /play")
                return@listenOnly
            }

            Logger.warn(subgame)

            val args = subgame.split(" ")
            val isSpectating = args.size > 1 && args[1].toBoolean()

            if (!isSpectating && !GameManager.getRegisteredNames().contains(args[0])) {
                Logger.error("${args[0]} game is not on this server")
                player.kick("${args[0]} game is not on this server. Try /play")
                return@listenOnly
            }

            if (isSpectating) {
                val playerToSpectate = Manager.connection.getPlayer(UUID.fromString(args[2]))
                if (playerToSpectate == null) {
                    Logger.warn("Player to spectate was null")
                    player.kick("That player is not online")
                    return@listenOnly
                }
                val game = playerToSpectate.game
                if (game == null) {
                    player.kick("That player is not in a game")
                    return@listenOnly
                }

                player.setTag(GameManager.playerSpectatingTag, true)
                player.respawnPoint = game.getSpawnPosition(player, spectator = true)

                setSpawningInstance(game.instance!!)
            } else {
                val newGameFuture = GameManager.findOrCreateGame(player, args[0])

                if (newGameFuture == null) {
                    Logger.warn("Game failed to create")
                    player.kick("Game was null")
                    return@listenOnly
                }

                val newGame = newGameFuture.join()

                newGame.playerCount.incrementAndGet()

//                Logger.info("player ${player.username} joining game ${newGame.id} with ${newGame.players.size} players")
//                Logger.info("game is joinable? ${newGame.canBeJoined(player)}")

                val respawnPoint = newGame.getSpawnPosition(player, spectator = false)
                player.respawnPoint = respawnPoint
                setSpawningInstance(newGame.instance!!)
            }
        }

        @Suppress("UnstableApiUsage")
        eventNode.listenOnly<PlayerChunkUnloadEvent> {
            if (instance.hasTag(GameManager.doNotAutoUnloadChunkTag)) return@listenOnly

            val index = instance.getTag(GameManager.doNotUnloadChunksIndex)
            if (index != null) {
                val spawnX = ChunkUtils.getChunkCoordX(index)
                val spawnZ = ChunkUtils.getChunkCoordZ(index)
                val radius = instance.getTag(GameManager.doNotUnloadChunksRadius) ?: 2
                if ( // If chunk is in radius...
                    !(chunkX > spawnX + radius
                            && chunkX < spawnX - radius
                            && chunkZ > spawnZ + radius
                            && chunkZ < spawnZ - radius)
                ) {
                    // ...do not unload it
                    return@listenOnly
                }
            }

            val chunk = instance.getChunk(chunkX, chunkZ) ?: return@listenOnly

            if (chunk.viewers.isEmpty()) {
                instance.unloadChunk(chunkX, chunkZ)
            }
        }

        val globalEvent = Manager.globalEvent

        globalEvent.listenOnly<PlayerMoveEvent> {
            // Teleport before player dies from void
            if (player.position.y < -20) {
                player.teleport(player.respawnPoint)
            }
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (instance.getBlock(this.blockPosition).isSolid) {
                isCancelled = true
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.removeTag(GameManager.joiningGameTag)

            val viewingNpcs = (PacketNPC.viewerMap[player.uuid] ?: return@listenOnly).toMutableList()
            viewingNpcs.forEach {
                it.removeViewer(player)
            }
            PacketNPC.viewerMap.remove(player.uuid)
        }

        eventNode.listenOnly<PlayerStartSneakingEvent> {
            if (player.gameMode == GameMode.SPECTATOR) {
                this.player.stopSpectating()
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
            if (this.isFirstSpawn) {
                player.resetTeam()

                if (player.hasLuckPermission("*")) {
                    player.permissionLevel = 4
                }

                PermissionUtils.refreshPrefix(player)
            } else {
                // To mutable list here to copy list in order to avoid concurrent modification and unsupported operation
                val viewingNpcs = (PacketNPC.viewerMap[player.uuid] ?: return@listenOnly).toMutableList()
                viewingNpcs.forEach {
                    it.removeViewer(player)
                }
            }
        }
    }

}