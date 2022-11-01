package dev.emortal.immortal.game

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameTypeInfo
import dev.emortal.immortal.util.JedisStorage
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
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


    val registeredClassMap = ConcurrentHashMap<KClass<out Game>, String>()
    val registeredGameMap = ConcurrentHashMap<String, GameTypeInfo>()

    private val gameIdMap = ConcurrentHashMap<Int, Game>()
    private val gameListMap = ConcurrentHashMap<String, MutableSet<Int>>()
    private val playerGameMap = ConcurrentHashMap<UUID, Int>()

    val Player.game: Game? get() = getPlayerGame(uuid)

    //fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    fun getGameById(id: Int): Game? = gameIdMap[id]
    fun getGameIds(gameName: String): MutableSet<Int>? = gameListMap[gameName]
    fun getGames(gameName: String): List<Game>? = getGameIds(gameName)?.mapNotNull { getGameById(it) }
    fun getPlayerGame(uuid: UUID): Game? = gameIdMap[playerGameMap[uuid]]


    private fun handleJoin(player: Player, lastGame: Game?, newGame: Game, spectate: Boolean = false, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && player.hasTag(joiningGameTag)) return

        lastGame?.removePlayer(player)
        lastGame?.removeSpectator(player)
        playerGameMap.remove(player.uuid)

        if (spectate) newGame.addSpectator(player) else newGame.addPlayer(player)

        playerGameMap[player.uuid] = newGame.id

        player.scheduler().buildTask {
            player.removeTag(joiningGameTag)
        }.delay(Duration.ofSeconds(1)).schedule()
    }

    fun Player.joinGame(game: Game, spectate: Boolean = false, ignoreCooldown: Boolean = false): Boolean {
        setTag(joiningGameTag, true)

        if (!game.canBeJoined(this) && !spectate) {
            Logger.warn("Game could not be joined")
            return false
        }
        if (spectate && !game.allowsSpectators) {
            Logger.warn("Attempted spectate but game is not spectatable")
            return false
        }

        val lastGame = this.game
        handleJoin(this, lastGame, game, spectate, ignoreCooldown)
        return true
    }

    fun Player.leaveGame() {
        val playerGame = game

        playerGame?.removePlayer(this)
        playerGame?.removeSpectator(this)

        playerGameMap.remove(this.uuid)
        removeTag(joiningGameTag)
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        ignoreCooldown: Boolean = false
    ) {
        if (!ignoreCooldown && hasTag(joiningGameTag)) return

        val gameFuture = findOrCreateGame(this, gameTypeName)

        if (gameFuture == null) {
            Logger.error("Game was null")
            return
        }

        gameFuture.thenAccept { game ->
            joinGame(game, ignoreCooldown = ignoreCooldown)
        }
    }

    fun findOrCreateGame(
        player: Player,
        gameTypeName: String
    ): CompletableFuture<Game>? {
        val firstAvailableGame = getGames(gameTypeName)?.firstOrNull {
            it.canBeJoined(player)
        }

        if (firstAvailableGame != null) {
            return firstAvailableGame.create()?.thenApply { firstAvailableGame }
        }

        // No games are available, we need to create a new game
        return createGame(gameTypeName)
    }

    fun createGame(gameTypeName: String): CompletableFuture<Game>? {
        val game = registeredGameMap[gameTypeName]?.clazz?.primaryConstructor?.call()
            ?: throw IllegalArgumentException("Primary constructor not found.")

        gameListMap[gameTypeName]!!.add(game.id)
        gameIdMap[game.id] = game

        return game.create()?.thenApply { game }
    }

    fun removeGame(game: Game) {
        gameIdMap.remove(game.id)
        gameListMap[game.gameName]!!.remove(game.id)
        game.players.forEach {
            playerGameMap.remove(it.uuid)
        }
    }

    inline fun <reified T : Game> registerGame(
        name: String,
        title: Component,
        showsInSlashPlay: Boolean = true,
    ) = registerGame(T::class, name, title, showsInSlashPlay)

    fun registerGame(
        clazz: KClass<out Game>,
        name: String,
        title: Component,
        showsInSlashPlay: Boolean = true
    ) = runBlocking {
        registeredClassMap[clazz] = name
        registeredGameMap[name] = GameTypeInfo(
            clazz,
            name,
            title,
            showsInSlashPlay
        )

        JedisStorage.jedis?.publish("registergame", "$name ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")

        gameListMap[name] = ConcurrentHashMap.newKeySet()

        Logger.info("Registered game type '${name}'")
    }
}