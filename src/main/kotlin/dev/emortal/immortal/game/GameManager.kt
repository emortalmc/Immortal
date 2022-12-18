package dev.emortal.immortal.game

import dev.emortal.immortal.Immortal
import dev.emortal.immortal.config.GameTypeInfo
import dev.emortal.immortal.util.JedisStorage
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import net.minestom.server.tag.Taggable
import org.tinylog.kotlin.Logger
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
    val playerSpectatingTag = Tag.Boolean("spectating")

    private val registeredClassMap = ConcurrentHashMap<KClass<out Game>, String>()
    private val registeredGameMap = ConcurrentHashMap<String, GameTypeInfo>()

    private val gameIdMap = ConcurrentHashMap<Int, Game>()
    private val gameListMap = ConcurrentHashMap<String, MutableSet<Int>>()

    val Player.game: Game?
        get() = getPlayerGame(this)

    //fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    fun getGameName(clazz: KClass<out Game>): String? = registeredClassMap[clazz]

    fun getGameTypeInfo(gameName: String): GameTypeInfo? = registeredGameMap[gameName]
    fun getRegisteredNames(): Set<String> = gameListMap.keys.toSet()

    fun getGameById(id: Int): Game? = gameIdMap[id]
    fun getGameIds(gameName: String): MutableSet<Int>? = gameListMap[gameName]
    fun getGames(gameName: String): List<Game>? = getGameIds(gameName)?.mapNotNull { getGameById(it) }

    fun getPlayerGame(player: Player): Game? = player.instance?.let { getGameByTaggable(it) }
    fun getGameByTaggable(taggable: Taggable): Game? = gameIdMap[taggable.getTag(gameIdTag)]

    fun Player.joinGameOrNew(
        gameTypeName: String,
        hasCooldown: Boolean = false
    ): CompletableFuture<Game>? {
        if (hasCooldown && hasTag(joiningGameTag)) return null

        val gameFuture = findOrCreateGame(this, gameTypeName)

        if (gameFuture == null) {
            Logger.error("Game was null")
            return null
        }

        return gameFuture.thenApply { game ->
            val spawnPosition = game.getSpawnPosition(this)
            this.respawnPoint = spawnPosition
            this.setInstance(game.instance!!, spawnPosition)
            game
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
            val future = CompletableFuture<Game>()

            firstAvailableGame.create()?.thenRun {
                future.complete(firstAvailableGame)
            }

            return future
        }

        // No games are available, we need to create a new game
        return createGame(gameTypeName)

    }

    fun createGame(gameTypeName: String): CompletableFuture<Game>? {
        val game = registeredGameMap[gameTypeName]?.clazz?.primaryConstructor?.call()
            ?: throw IllegalArgumentException("Primary constructor not found.")

        gameListMap[gameTypeName]!!.add(game.id)
        gameIdMap[game.id] = game

        val future = CompletableFuture<Game>()

        val gameCreate = game.create() ?: return null
        gameCreate.thenRun {
            future.complete(game)
        }

        return future
    }

    fun removeGame(game: Game) {
        gameIdMap.remove(game.id)
        gameListMap[game.gameName]!!.remove(game.id)

        Logger.info("Game was removed. Game list map is ${gameListMap.size} big")
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

        JedisStorage.jedis?.publish("registergame", "$name ${Immortal.gameConfig.serverName} ${Immortal.port} ${showsInSlashPlay}")

        gameListMap[name] = ConcurrentHashMap.newKeySet()

        Logger.info("Registered game type '${name}'")
    }
}