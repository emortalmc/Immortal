package dev.emortal.immortal.game

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.config.GameTypeInfo
import dev.emortal.immortal.pool.GameQueue
import dev.emortal.immortal.pool.Pool
import dev.emortal.immortal.util.JedisStorage
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor


object GameManager {

    // Instance Tags
    val doNotUnregisterTag = Tag.Boolean("doNotUnregister")
    val doNotAutoUnloadChunkTag = Tag.Boolean("doNotAutoUnloadChunk")
    val doNotUnloadChunksIndex = Tag.Long("doNotUnloadChunksIndex")
    val doNotUnloadChunksRadius = Tag.Integer("doNotUnloadChunksRadius")

    // Game Tags
    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")
    val joiningGameTag = Tag.Boolean("joiningGame")

    val playerGameMap = ConcurrentHashMap<UUID, Game>()
    val registeredClassMap = ConcurrentHashMap<KClass<out Game>, String>()
    val registeredGameMap = ConcurrentHashMap<String, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, ConcurrentHashMap<Int, Game>>()
    val gamePoolMap = ConcurrentHashMap<String, GameQueue>()

    val Player.game get() = playerGameMap[this.uuid]

    private val lastGameId = AtomicInteger(0)
    fun getNextGameId() = lastGameId.getAndIncrement()

    //fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    private fun handleJoin(player: Player, lastGame: Game?, newGame: Game, spectate: Boolean = false, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && player.hasTag(joiningGameTag)) return

        Logger.info("Attempting to join ${newGame.gameName}")

        lastGame?.removePlayer(player)
        lastGame?.removeSpectator(player)
        //playerGameMap.remove(player.uuid)

        if (spectate) newGame.addSpectator(player) else newGame.addPlayer(player)

        playerGameMap[player.uuid] = newGame

        player.scheduler().buildTask {
            player.removeTag(joiningGameTag)
        }.delay(Duration.ofSeconds(1)).schedule()
    }

    fun Player.joinGame(game: Game, spectate: Boolean = false, ignoreCooldown: Boolean = false): Boolean {
        setTag(joiningGameTag, true)

        val gameName = registeredClassMap[game::class]!!
        val gameTypeInfo = registeredGameMap[gameName] ?: throw Error("Game type not registered")

        if (!game.canBeJoined(this) && !spectate) {
            Logger.warn("Game could not be joined")
            return false
        }
        if (spectate && !gameTypeInfo.spectatable) {
            Logger.warn("Attempted spectate but game is not spectatable")
            return false
        }

        val lastGame = this.game
        handleJoin(this, lastGame, game, spectate, ignoreCooldown)
        return true
    }

    fun Player.leaveGame() {
        game?.removePlayer(this)
        game?.removeSpectator(this)
        playerGameMap.remove(this.uuid)
        removeTag(joiningGameTag)
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]!!.options,
        ignoreCooldown: Boolean = false
    ) {
        if (!ignoreCooldown && hasTag(joiningGameTag)) return

        val game = findOrCreateGame(this, gameTypeName, options)

        if (game == null) {
            Logger.error("Game was null")
            return
        }

        joinGame(game, ignoreCooldown = ignoreCooldown)
    }

    fun findOrCreateGame(
        player: Player,
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]!!.options
    ): Game? {
        return gamePoolMap[gameTypeName]?.next(player)


    }

    fun createGame(gameTypeName: String, options: GameOptions): Game {
        val game = registeredGameMap[gameTypeName]?.clazz?.primaryConstructor?.call(options)
            ?: throw IllegalArgumentException("Primary constructor not found.")

        gameMap[gameTypeName]?.put(game.id, game)

        return game
    }

    inline fun <reified T : Game> registerGame(
        name: String,
        title: Component,
        showsInSlashPlay: Boolean = true,
        canSpectate: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        options: GameOptions
    ) = registerGame(T::class, name, title, showsInSlashPlay, canSpectate, whenToRegisterEvents, options)

    fun registerGame(
        clazz: KClass<out Game>,
        name: String,
        title: Component,
        showsInSlashPlay: Boolean = true,
        canSpectate: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        options: GameOptions
    ) = runBlocking {
        registeredClassMap[clazz] = name
        registeredGameMap[name] = GameTypeInfo(
            clazz,
            name,
            title,
            showsInSlashPlay,
            canSpectate,
            whenToRegisterEvents,
            options
        )

        JedisStorage.jedis?.publish("registergame", "$name ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")

        gameMap[name] = ConcurrentHashMap()
        gamePoolMap[name] = GameQueue(Pool.dynamic({ createGame(name, options) }, { gameMap[name]!!.count { it.value.players.isNotEmpty() } }))

        Logger.info("Registered game type '${name}'")
    }
}