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
import dev.emortal.immortal.util.CoroutineRunnable
import dev.emortal.immortal.util.RedisStorage.redisson
import dev.emortal.immortal.util.resetTeam
import kotlinx.coroutines.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
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
import net.minestom.server.network.packet.client.play.ClientSetRecipeBookStatePacket
import net.minestom.server.timer.TaskSchedule
import net.minestom.server.utils.NamespaceID
import net.minestom.server.world.DimensionType
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
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

            // Ignore warning when player opens recipe book
            Manager.packetListener.setListener(ClientSetRecipeBookStatePacket::class.java) { _: ClientSetRecipeBookStatePacket, _: Player -> }

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

                        player.scheduleNextTick {
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

                            player.scheduleNextTick {
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

            val joinsScope = CoroutineScope(Dispatchers.IO)

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
                    player.scheduleNextTick {
                        joinsScope.launch {
                            player.joinGame(game, spectate = true)
                        }
                    }
                } else {
                    val newGame = GameManager.findOrCreateGame(player, args[0])

                    if (!newGame.instance.isRegistered) Manager.instance.registerInstance(newGame.instance)

                    setSpawningInstance(newGame.instance)

                    player.respawnPoint = newGame.spawnPosition
                    player.scheduleNextTick {
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

            globalEvent.listenOnly<PlayerMoveEvent> {
                if (player.position.y < -64) {
                    // anti void death lul
                    player.teleport(player.respawnPoint)
                }
            }

//            val technoSkin = PlayerSkin("ewogICJ0aW1lc3RhbXAiIDogMTYxODQxMTkwNzYwMiwKICAicHJvZmlsZUlkIiA6ICJiODc2ZWMzMmUzOTY0NzZiYTExNTg0MzhkODNjNjdkNCIsCiAgInByb2ZpbGVOYW1lIiA6ICJUZWNobm9ibGFkZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83ODZjMDM5ZDk2OWQxODM5MTU1MjU1ZTM4ZTdiMDZhNjI2ZWE5ZjhiYWY5Y2I1NWUwYTc3MzExZWZlMThhM2UiCiAgICB9CiAgfQp9", "MYwB5ulhwIk1otEtouH6m2gG5rfJOIuOnJbhGZ4O+btvDvIDBwBbP1KQ5W3XWn6IMZTBfZE7SSgfTlX0o93diYc/fDwk1x/geyI8bnjRt78YRptxlwncHq6jKSMBSXX4JhkMyrZTtdw8C39ojYjty9O9Ly7kJU0fogKV1pi4hLz8E5tPdQz3/vAXlpTzhAVOsQ9Za6qEykLQRXtgQoS2+/7n4fQWldymhGSlxF+dFCbE2K19einXvP783bUoAsu6AOQxLewO92xICbjRdPvM09c/4LqxsvLebqtYIoPEIuAtOn20doWBL71fLfld8TPEUdoo2V8IAooHWXvIgmw1pTjCw63oPixB/Uaq4SuUnCq6/dopGiMDThXJO+ALK12IouJ/CrgzwgJc4VEXPx8IRoDW07yqoPYfiQ/XyjKsO1K+LQC4uai764jEXtiZ9W3sX/Nj0ENO2O+BfM1AsrxZj7SLEWj4uV57g2LyYrs27fctMpncU0QIpz4WL0NPTKh5nor+9Z6ilKHmxoxaXg8A1V5JcNpoe0q+yQNmEKK4HCFYTZZnoMYWv+SQsanhqH3Z6Y/MLZm0eYeZt+FOb6oqou+2JSVdtQZmS6wg8ec6YcCNOG0EoqxS57cHjFaj/Lcw8ZH37jB7Q87GR1mAGmptJtuukCFMp3uTvp4I2O6Nxaw=")
//
//            object : MinestomRunnable(coroutineScope = GlobalScope, repeat = Duration.ofSeconds(2)) {
//                override suspend fun run() {
//                    val rand = ThreadLocalRandom.current()
//                    Manager.connection.onlinePlayers.forEach {
//                        if (rand.nextDouble() < 0.5) {
//                            it.instance!!.playSound(Sound.sound(SoundEvent.ENTITY_PIG_AMBIENT, Sound.Source.MASTER, 1f, rand.nextFloat(0.85f, 1.15f)), it.position)
//                        }
//                    }
//                }
//            }

//            eventNode.listenOnly<PlayerSkinInitEvent> {
//                this.skin = technoSkin
//            }

            eventNode.listenOnly<RemoveEntityFromInstanceEvent> {
                if (this.entity !is Player) return@listenOnly

                this.instance.scheduler().buildTask {
                    if (instance.players.isEmpty()) {
                        if (instance.hasTag(GameManager.doNotUnregisterTag)) return@buildTask
                        //if (!instance.isRegistered) return@buildTask
                        instanceManager.unregisterInstance(instance)
                        Logger.info("Instance was unregistered. Instance count: ${instanceManager.instances.size}")
                    } else {
                        //Logger.warn("Couldn't unregister instance, players: ${it.players.joinToString(separator = ", ") { it.username }}")
                    }
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

                if ((player.gameMode == GameMode.ADVENTURE && !player.getItemInHand(hand).meta().canPlaceOn.contains(blockPlacedOn)) || player.gameMode == GameMode.SPECTATOR) {
                    isCancelled = true
                }
            }
            eventNode.listenOnly<PlayerBlockBreakEvent> {
                if ((player.gameMode == GameMode.ADVENTURE && !player.itemInMainHand.meta().canDestroy.contains(block)) || player.gameMode == GameMode.SPECTATOR) {
                    isCancelled = true
                }
            }

            if (debugMode) {
                val LAST_TICK = AtomicReference<TickMonitor>()

                eventNode.listenOnly<ServerTickMonitorEvent> {
                    LAST_TICK.set(tickMonitor)
                }

                object : CoroutineRunnable(coroutineScope = GlobalScope, repeat = Duration.ofMillis(500)) {
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