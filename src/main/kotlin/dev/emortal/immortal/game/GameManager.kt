package dev.emortal.immortal.game

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.config.GameTypeInfo
import dev.emortal.immortal.util.RedisStorage.redisson
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import net.minestom.server.tag.Tag
import org.tinylog.kotlin.Logger
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val doNotUnregisterTag = Tag.Boolean("doNotUnregister")
    val doNotAutoUnloadChunkTag = Tag.Boolean("doNotAutoUnloadChunk")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")
    val joiningGameTag = Tag.Boolean("joiningGame")
    val doNotTeleportTag = Tag.Boolean("doNotTeleport")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val registeredClassMap = ConcurrentHashMap<KClass<out Game>, String>()
    val registeredGameMap = ConcurrentHashMap<String, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    val Player.game get() = playerGameMap[this]

    //val canBeJoinedLock = Object()

    private val lastGameId = AtomicInteger(0)
    fun getNextGameId() = lastGameId.getAndIncrement()

    //fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    private fun handleJoin(player: Player, lastGame: Game?, newGame: Game, spectate: Boolean = false, ignoreCooldown: Boolean = false) {
        if (!ignoreCooldown && player.hasTag(joiningGameTag)) return

        Logger.info("Attempting to join ${newGame.gameName}")

        lastGame?.removePlayer(player)
        lastGame?.removeSpectator(player)
        playerGameMap.remove(player)

        if (spectate) newGame.addSpectator(player) else newGame.addPlayer(player)

        playerGameMap[player] = newGame

        player.scheduler().buildTask {
            player.removeTag(joiningGameTag)
        }.delay(Duration.ofSeconds(1)).schedule()
    }

    fun Player.joinGame(game: Game, spectate: Boolean = false): Boolean {
        if (!game.canBeJoined(this) && !spectate) {
            Logger.warn("Game could not be joined")
            return false
        }

        val lastGame = this.game
        handleJoin(this, lastGame, game, spectate)
        return true
    }

    fun Player.leaveGame() {
        game?.removePlayer(this)
        playerGameMap.remove(this)
        removeTag(joiningGameTag)
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]!!.options
    ) {
        joinGame(findOrCreateGame(this, gameTypeName, options))
        setTag(joiningGameTag, true)
    }

    @Synchronized
    fun findOrCreateGame(
        player: Player,
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]!!.options
    ): Game {
        return gameMap[gameTypeName]?.firstOrNull {
            it.canBeJoined(player) && it.gameOptions == options
        }
            ?: createGame(gameTypeName, options, player)

    }

    fun createGame(gameTypeName: String, options: GameOptions, creator: Player? = null): Game {

        //options.private = creator?.party?.privateGames ?: false

        val game = registeredGameMap[gameTypeName]?.clazz?.primaryConstructor?.call(options)
            ?: throw IllegalArgumentException("Primary constructor not found.")
        //game.gameCreator = creator

        gameMap[gameTypeName]?.add(game)

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
    ) {
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

        redisson?.getTopic("registergame")
            ?.publishAsync("$name ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")

        gameMap[name] = ConcurrentHashMap.newKeySet()

        Logger.info("Registered game type '${name}'")
    }
}