package dev.emortal.immortal

import dev.emortal.immortal.blockhandler.SignHandler
import dev.emortal.immortal.blockhandler.SkullHandler
import dev.emortal.immortal.commands.*
import dev.emortal.immortal.config.ConfigHelper
import dev.emortal.immortal.config.GameConfig
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import dev.emortal.immortal.luckperms.LuckpermsListener
import dev.emortal.immortal.luckperms.PermissionUtils
import dev.emortal.immortal.luckperms.PermissionUtils.hasLuckPermission
import dev.emortal.immortal.luckperms.PermissionUtils.lpUser
import dev.emortal.immortal.npc.PacketNPC
import dev.emortal.immortal.util.RedisStorage.redisson
import dev.emortal.immortal.util.SuperflatGenerator
import dev.emortal.immortal.util.resetTeam
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.extensions.Extension
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register
import java.nio.file.Path
import java.time.Duration
import java.util.*

class ImmortalExtension : Extension() {

    companion object {
        lateinit var luckperms: LuckPerms

        lateinit var gameConfig: GameConfig
        val configPath = Path.of("./immortalconfig.json")

        fun init(eventNode: EventNode<Event> = Manager.globalEvent) = CoroutineScope(Dispatchers.IO).launch {
            gameConfig = ConfigHelper.initConfigFile(configPath, GameConfig("replaceme", 42069))

            MinecraftServer.setBrandName("Minestom ${MinecraftServer.VERSION_NAME}")

            val debugMode = System.getProperty("debug").toBoolean()
            val debugGame = System.getProperty("debuggame")
            if (debugMode) Logger.warn("Running in debug mode, debug game: ${debugGame}")

            val instanceManager = Manager.instance

            val waitingInstance = instanceManager.createInstanceContainer()
            waitingInstance.chunkGenerator = SuperflatGenerator
            waitingInstance.setTag(GameManager.doNotUnregisterTag, 1)

            // Create proxyhello listener
            redisson?.getTopic("proxyhello")?.addListenerAsync(String::class.java) { ch, msg ->
                val registerTopic = redisson.getTopic("registergame")
                GameManager.gameMap.keys.forEach {
                    Logger.info("Received proxyhello, resending register request $it")
                    registerTopic.publishAsync("$it ${gameConfig.serverName} ${gameConfig.serverPort}")
                }
            }

            // Create changegame listener
            redisson?.getTopic("playerpubsub${gameConfig.serverName}")?.addListenerAsync(String::class.java) { ch, msg ->
                val args = msg.split(" ")

                when (args[0].lowercase()) {
                    "changegame" -> {
                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return@addListenerAsync
                        val subgame = args[2]
                        if (!GameManager.gameMap.containsKey(subgame)) {
                            // Invalid subgame, ignore message
                            Logger.warn("Invalid subgame $subgame")
                            return@addListenerAsync
                        }

                        if (player.game?.gameName == subgame) return@addListenerAsync

                        CoroutineScope(Dispatchers.IO).launch {
                            player.joinGameOrNew(subgame)
                        }
                    }

                }
            }


            eventNode.listenOnly<PlayerLoginEvent> {
                this.player.gameMode = GameMode.ADVENTURE

                // Read then delete value
                var subgame = redisson?.getBucket<String>("${player.uuid}-subgame")?.andDelete

                if (System.getProperty("debug") == "true") {
                   subgame = System.getProperty("debuggame")
                }

                if (subgame == null) {
                    this.player.kick("Error while joining")
                    Logger.info("Player ${player.username} unable to connect because subgame was null")
                    return@listenOnly
                }

                val newGame = GameManager.findOrCreateGame(player, subgame)
                player.respawnPoint = newGame.spawnPosition
                setSpawningInstance(newGame.instance)

                player.scheduleNextTick {
                    player.joinGame(newGame)
                }
            }

            eventNode.listenOnly<PlayerChunkUnloadEvent> {
                val chunk = instance.getChunk(chunkX, chunkZ) ?: return@listenOnly

                if (chunk.viewers.isEmpty()) {
                    try {
                        instance.unloadChunk(chunkX, chunkZ)
                    } catch (_: NullPointerException) {}
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

//            val cooldown = Duration.ofMinutes(60)
//            Manager.scheduler.buildTask {
//
//                var leftoverInstances = 0
//                instanceManager.instances.forEach {
//                    if (it.players.isEmpty()) {
//                        if (it.hasTag(GameManager.doNotUnregisterTag)) return@forEach
//
//                        instanceManager.unregisterInstance(it)
//                        leftoverInstances++
//                    }
//                }
//                if (leftoverInstances != 0) Logger.warn("$leftoverInstances empty instances were found and unregistered.")
//            }.delay(cooldown).repeat(cooldown).schedule()

            eventNode.listenOnly<RemoveEntityFromInstanceEvent> {
                if (this.entity !is Player) return@listenOnly

                this.instance.scheduleNextTick {
                    if (it.players.isEmpty()) {
                        if (it.hasTag(GameManager.doNotUnregisterTag)) return@scheduleNextTick
                        if (!it.isRegistered) return@scheduleNextTick
                        instanceManager.unregisterInstance(it)
                        Logger.info("Instance was unregistered. Instance count: ${instanceManager.instances.size}")
                    } else {
                        //Logger.warn("Couldn't unregister instance, players: ${it.players.joinToString(separator = ", ") { it.username }}")
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
            SoundCommand.register()
            StatsCommand.register()
            ListCommand.register()
            VersionCommand.register()

            Logger.info("Immortal initialized!")
        }
    }

    override fun initialize(): Unit = runBlocking {
        luckperms = LuckPermsProvider.get()
        LuckpermsListener(this@ImmortalExtension, luckperms)
        init()
    }

    override fun terminate() {
        ForceStartCommand.unregister()
        SoundCommand.unregister()
        StatsCommand.unregister()
        ListCommand.unregister()
        VersionCommand.unregister()

        redisson?.shutdown()

        Logger.info("Immortal terminated!")
    }

}