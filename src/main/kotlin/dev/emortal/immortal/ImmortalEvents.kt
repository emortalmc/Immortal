package dev.emortal.immortal

import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.luckperms.PermissionUtils.lpUser
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.RedisStorage
import dev.emortal.immortal.util.resetTeam
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.block.Block
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.timer.Task
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.chunk.ChunkUtils
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ImmortalEvents {

    @Suppress("UnstableApiUsage")
    fun register(eventNode: EventNode<Event>) {
        eventNode.listenOnly<PlayerLoginEvent> {
            this.player.gameMode = GameMode.ADVENTURE

            val subgame = if (System.getProperty("debug") == "true") {
                System.getProperty("debuggame")
            } else {
                // Read then delete value
                RedisStorage.redisson?.getBucket<String>("${player.uuid}-subgame")?.andDelete?.trim()
            }

            if (subgame == null) return@listenOnly

            val args = subgame.split(" ")
            val isSpectating = args.size > 1 && args[1].toBoolean()

            Logger.info(subgame)
            if (isSpectating) {
                Logger.info("Spectating!")

                val playerToSpectate = Manager.connection.getPlayer(UUID.fromString(args[2]))
                if (playerToSpectate == null) {
                    Logger.warn("Player to spectate was null")
                    this.player.kick("That player is not online")
                    return@listenOnly
                }
                val game = playerToSpectate.game
                if (game == null) {
                    Logger.warn("Player's game was null")
                    this.player.kick("That player is not on a game")
                    return@listenOnly
                }
                player.respawnPoint = game.spawnPosition
                setSpawningInstance(game.instance)
                player.scheduleNextTick {
                    player.joinGame(game, spectate = true)
                }
            } else {
                val newGame = GameManager.findOrCreateGame(player, args[0])
                newGame.queuedPlayers.add(player)
                if (!newGame.instance.isRegistered) Manager.instance.registerInstance(newGame.instance)

                setSpawningInstance(newGame.instance)

                player.respawnPoint = newGame.spawnPosition
                player.scheduleNextTick {
                    player.joinGame(newGame)
                }

            }
        }

        val unloadingSoon = ConcurrentHashMap<Long, Task>()

        eventNode.listenOnly<PlayerChunkLoadEvent> {
            val index = ChunkUtils.getChunkIndex(chunkX, chunkZ)

            unloadingSoon[index]?.cancel()
            unloadingSoon.remove(index)
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

            unloadingSoon[ChunkUtils.getChunkIndex(chunkX, chunkZ)] = instance.scheduler().buildTask {
                if (chunk.viewers.isEmpty()) {
                    try {
                        instance.unloadChunk(chunkX, chunkZ)
                    } catch (_: NullPointerException) {}
                }
            }.delay(TaskSchedule.tick(5)).schedule()

        }

        val globalEvent = Manager.globalEvent

        globalEvent.listenOnly<PlayerMoveEvent> {
            // Teleport before player dies from void
            if (player.position.y < -64) {
                player.teleport(player.respawnPoint)
            }
        }

        eventNode.listenOnly<RemoveEntityFromInstanceEvent> {
            if (this.entity !is Player) return@listenOnly

            this.instance.scheduler().buildTask {
                if (instance.players.isNotEmpty() || !instance.isRegistered) return@buildTask

                if (instance.hasTag(GameManager.doNotUnregisterTag)) return@buildTask
                Manager.instance.unregisterInstance(instance)
                if (instance is InstanceContainer) (instance as InstanceContainer).chunkLoader = null
            }.delay(TaskSchedule.nextTick()).schedule()
        }

        eventNode.listenOnly<PlayerBlockPlaceEvent> {
            if (!instance.getBlock(this.blockPosition).isAir) {
                isCancelled = true
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

        eventNode.listenOnly<PlayerStartSneakingEvent> {
            if (player.gameMode == GameMode.SPECTATOR) {
                this.player.stopSpectating()
            }
        }

        eventNode.listenOnly<PlayerSpawnEvent> {
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
            val dir = blockFace.toDirection()
            val placedOnPos = blockPosition.sub(dir.normalX().toDouble(), dir.normalY().toDouble(), dir.normalZ().toDouble())
            val blockPlacedOn = instance.getBlock(placedOnPos)


            if (player.gameMode == GameMode.ADVENTURE && player.getItemInHand(hand).meta().canPlaceOn.none { Block.fromNamespaceId(it)!!.compare(blockPlacedOn) } || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }
        eventNode.listenOnly<PlayerBlockBreakEvent> {

            if (player.gameMode == GameMode.ADVENTURE && player.itemInMainHand.meta().canDestroy.none { Block.fromNamespaceId(it)!!.compare(block) } || player.gameMode == GameMode.SPECTATOR) {
                isCancelled = true
            }
        }
    }

}