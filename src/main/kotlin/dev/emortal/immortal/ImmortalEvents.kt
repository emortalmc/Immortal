package dev.emortal.immortal

import dev.emortal.immortal.game.Game
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameManager.leaveGame
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.JedisStorage
import dev.emortal.immortal.util.resetTeam
import net.minestom.server.entity.GameMode
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.utils.chunk.ChunkUtils
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ImmortalEvents {

    @Suppress("UnstableApiUsage")
    fun register(eventNode: EventNode<Event>) {
        val preparedGameMap = ConcurrentHashMap<UUID, Pair<Game, Boolean>>()

        eventNode.listenOnly<AsyncPlayerPreLoginEvent> {
            var subgame = if (System.getProperty("debug") == "true") {
                System.getProperty("debuggame")
            } else {
                // Read then delete value
                JedisStorage.jedis?.getDel("${playerUuid}-subgame")?.trim()
            }

            if (subgame == null && GameManager.registeredGameMap.keys.size == 1) {
                subgame = GameManager.registeredGameMap.keys.first()
            }

            if (subgame == null) {
                Logger.error("Subgame is null!")
                player.kick("Game was not provided. Try /play")
                return@listenOnly
            }

            Logger.warn(subgame)

            val args = subgame.split(" ")
            val isSpectating = args.size > 1 && args[1].toBoolean()

            if (!isSpectating && !GameManager.registeredGameMap.containsKey(args[0])) {
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
//                player.respawnPoint = game.spawnPosition

                preparedGameMap[playerUuid] = game to true
            } else {
                val newGame = GameManager.findOrCreateGame(player, args[0])

//                instance.loadChunk(newGame.spawnPosition).thenAccept {
//                    if (it == null) {
//                        player.kick("Chunk is null")
//                        return@thenAccept
//                    }
//                }

                newGame.queuedPlayers.add(player)

                preparedGameMap[playerUuid] = newGame to false
            }
        }

        if (!System.getProperty("debug").toBoolean()) eventNode.listenOnly<PlayerChatEvent> {
            isCancelled = true
        }

        eventNode.listenOnly<PlayerLoginEvent> {
            player.gameMode = GameMode.ADVENTURE

            val preparedGame = preparedGameMap[player.uuid]
            preparedGameMap.remove(player.uuid)
            if (preparedGame == null) {
                player.kick("Game was not prepared")
                Logger.error("Game was not prepared")
                return@listenOnly
            }

            setSpawningInstance(preparedGame.first.instance)

            player.respawnPoint = preparedGame.first.getSpawnPosition(player, preparedGame.second)
            player.scheduleNextTick {
                player.joinGame(preparedGame.first, spectate = preparedGame.second, ignoreCooldown = true)
            }
        }

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
            if (player.position.y < -64) {
                player.teleport(player.respawnPoint)
            }
        }

        eventNode.listenOnly<RemoveEntityFromInstanceEvent> {
//            val player = entity as? Player ?: return@listenOnly

            if (instance.hasTag(GameManager.doNotUnregisterTag)) return@listenOnly

            this.instance.scheduleNextTick {
                if (instance.players.isNotEmpty() || !instance.isRegistered) return@scheduleNextTick

//                // Destroy the game associated with the instance
//                val gameName = instance.getTag(GameManager.gameNameTag)
//                val gameId = instance.getTag(GameManager.gameIdTag)
//                GameManager.gameMap[gameName]?.get(gameId)?.destroy()

                Manager.instance.unregisterInstance(instance)
            }
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (instance.getBlock(this.blockPosition).isSolid) {
                isCancelled = true
            }
        }

        eventNode.listenOnly<PlayerDisconnectEvent> {
            player.leaveGame()

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