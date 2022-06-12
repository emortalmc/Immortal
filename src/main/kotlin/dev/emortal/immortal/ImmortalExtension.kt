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
import dev.emortal.immortal.util.MinestomRunnable
import dev.emortal.immortal.util.RedisStorage.redisson
import dev.emortal.immortal.util.resetTeam
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent
import net.minestom.server.event.player.*
import net.minestom.server.event.server.ServerTickMonitorEvent
import net.minestom.server.extensions.Extension
import net.minestom.server.monitoring.TickMonitor
import net.minestom.server.network.packet.client.play.ClientInteractEntityPacket
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import world.cepi.kstom.adventure.asMini
import world.cepi.kstom.event.listenOnly
import world.cepi.kstom.util.register
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicReference

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

                    "spectateplayer" -> {
                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return@addListenerAsync
                        val playerToSpectate = Manager.connection.getPlayer((UUID.fromString(args[2]))) ?: return@addListenerAsync

                        val game = playerToSpectate.game
                        if (game != null) {
                            if (!game.gameOptions.allowsSpectators) {
                                player.sendMessage(Component.text("That game does not allow spectating", NamedTextColor.RED))
                                return@addListenerAsync
                            }
                            if (game.id == player.game?.id) {
                                player.sendMessage(Component.text("That player is not on a game", NamedTextColor.RED))
                                return@addListenerAsync
                            }
                            game.coroutineScope.launch {
                                player.joinGame(game, spectate = true)
                            }
                        }
                    }

                }
            }

            eventNode.listenOnly<AsyncPlayerPreLoginEvent> {
                val subgame = if (GameManager.registeredGameMap.size == 1) {
                    GameManager.registeredGameMap.entries.first().key
                } else if (System.getProperty("debug") == "true") {
                    System.getProperty("debuggame")
                } else {
                    redisson?.getBucket<String>("${player.uuid}-subgame")?.get()?.trim()
                }

                if (subgame == null) {
                    this.player.kick("Failed to join")
                    Logger.info("Player ${player.username} unable to connect because subgame was null")
                    return@listenOnly
                }

                val args = subgame.split(" ")
                val isSpectating = args.size > 1 && args[1].toBoolean()

                if (isSpectating && GameManager.registeredGameMap[subgame]?.options?.allowsSpectators == false) {
                    this.player.kick("Game does not allow spectators")
                }
                if (isSpectating) {
                    val playerToSpectate = Manager.connection.getPlayer(UUID.fromString(args[2]))
                    if (playerToSpectate == null) {
                        this.player.kick("That player is not online")
                        return@listenOnly
                    }
                    if (playerToSpectate.game == null) {
                        this.player.kick("That player is not on a game")
                        return@listenOnly
                    }
                }
            }

            val joinsScope = CoroutineScope(Dispatchers.Default)

            eventNode.listenOnly<PlayerLoginEvent> {
                this.player.gameMode = GameMode.ADVENTURE

                val subgame = if (System.getProperty("debug") == "true") {
                    System.getProperty("debuggame")
                } else {
                    // Read then delete value
                    redisson?.getBucket<String>("${player.uuid}-subgame")?.andDelete?.trim()
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
                    setSpawningInstance(game.instance)
                    joinsScope.launch {
                        player.joinGame(game, spectate = true)
                    }
                } else {
                    Logger.info("Finding game!")
                    joinsScope.launch {
                        val newGame = GameManager.findOrCreateGame(player, args[0])
                        setSpawningInstance(newGame.instance)

                        Logger.info("Found game and set instance!")

                        player.joinGame(newGame)
                    }

                }
            }

            eventNode.listenOnly<PlayerChunkUnloadEvent> {
                if (instance.hasTag(GameManager.doNotAutoUnloadChunkTag)) return@listenOnly
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
                if (player.position.y < -64) {
                    // anti void death lul
                    player.teleport(player.respawnPoint)
                }
            }



//            fun isRoughlyEqual(d1: Double, d2: Double) = abs(d1 - d2) < 0.001
//            val airTicksMap = ConcurrentHashMap<UUID, Int>()
//            val lastDistMap = ConcurrentHashMap<UUID, Double>()
//            val lastPosMap = ConcurrentHashMap<UUID, Pos>()



//            globalEvent.listenOnly<PlayerMoveEvent> {
//                val distY = newPosition.y - player.position.y
//                val lastDist = lastDistMap[player.uuid] ?: 0.0
//                val airTicks = airTicksMap[player.uuid] ?: 0
//                val predictedDistY = (lastDist - 0.08) * 0.98
//
//                if (!isOnGround && airTicks > 4 && abs(predictedDistY) >= 0.006
//                    && !player.isFlying
//                ) {
//
//                    if (!isRoughlyEqual(distY, predictedDistY)) {
//                        //player.teleport(lastPos)
//                        Logger.info("${player.username} triggered fly check")
//                        lastDistMap[player.uuid] = 0.0
//                        airTicksMap[player.uuid] = 0
//                    }
//                }
//
//                lastDistMap[player.uuid] = distY
//                if (isOnGround) {
//                    airTicksMap[player.uuid] = 0
//                } else {
//                    airTicksMap[player.uuid] = airTicks + 1
//                }
//
//                lastPosMap[player.uuid] = player.position
//            }

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

            if (debugMode) {
                val LAST_TICK = AtomicReference<TickMonitor>()

                eventNode.listenOnly<ServerTickMonitorEvent> {
                    LAST_TICK.set(tickMonitor)
                }

                object : MinestomRunnable(coroutineScope = GlobalScope, repeat = Duration.ofSeconds(1)) {
                    override suspend fun run() {
                        Manager.connection.onlinePlayers.forEach {
                            val ramUsage = Manager.benchmark.usedMemory / 1024 / 1024
                            val monitor = LAST_TICK.get()
                            val tickMs = Math.floor(monitor.tickTime * 100) / 100

                            val onlinePlayers = Manager.connection.onlinePlayers.size
                            val instances = Manager.instance.instances
                            val entities = instances.sumOf { it.entities.size } - onlinePlayers
                            val chunks = instances.sumOf { it.chunks.size }

                            it.sendPlayerListFooter(
                                Component.text()
                                    .append(Component.text(ramUsage, NamedTextColor.GOLD))
                                    .append(Component.text("MB", NamedTextColor.GOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(tickMs, NamedTextColor.GOLD))
                                    .append(Component.text("mspt", NamedTextColor.GOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(onlinePlayers, NamedTextColor.GOLD))
                                    .append(Component.text(" players", NamedTextColor.GOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(entities, NamedTextColor.GOLD))
                                    .append(Component.text(" entities", NamedTextColor.GOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(instances.size, NamedTextColor.GOLD))
                                    .append(Component.text(" instances", NamedTextColor.GOLD))
                                    .append(Component.text(" | ", NamedTextColor.DARK_GRAY))
                                    .append(Component.text(chunks, NamedTextColor.GOLD))
                                    .append(Component.text(" chunks", NamedTextColor.GOLD))
                            )
                        }
                    }
                }
            }

            Manager.scheduler.buildShutdownTask {
                val kickMessage = Component.text()
                    .append("<gradient:gold:light_purple><bold>EmortalMC".asMini())
                    .append(Component.text("\n\nServer shutting down", NamedTextColor.RED))
                    .append(Component.text("\n\n(prob won't be for long - minestom ftw)", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC))
                    .build()

                Manager.connection.onlinePlayers.forEach {
                    it.kick(kickMessage)
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
            SettingsCommand.register()
            if (debugMode) {
                DebugSpectateCommand.register()
            }

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
        SettingsCommand.unregister()
        DebugSpectateCommand.unregister()

        redisson?.shutdown()

        Logger.info("Immortal terminated!")
    }

}