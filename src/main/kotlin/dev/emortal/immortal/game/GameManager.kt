package dev.emortal.immortal.game

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameConfig
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.util.RedisStorage
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val logger: Logger = LoggerFactory.getLogger("GameManager")

    val doNotUnregisterTag = Tag.Byte("doNotUnregister")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")
    val joiningGameTag = Tag.Byte("joiningGame")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val gameNameToClassMap = ConcurrentHashMap<String, KClass<out Game>>()
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameConfig>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    @Volatile
    internal var nextGameID = 0

    val Player.game get() = playerGameMap[this]

    fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    @Synchronized private fun handleJoin(player: Player, lastGame: Game?, newGame: Game, future: CompletableFuture<Boolean>) = future.thenAcceptAsync { successful ->
        if (successful) {
            lastGame?.removePlayer(player)
            lastGame?.removeSpectator(player)
            playerGameMap[player] = newGame

            Manager.scheduler.buildTask {
                player.removeTag(joiningGameTag)
            }.delay(Duration.ofSeconds(1)).schedule()
        } else {
            player.sendMessage(Component.text("Something went wrong while spectating ${newGame.gameTypeInfo.gameName}", NamedTextColor.RED))
            player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

            player.removeTag(joiningGameTag)
        }
    }

    @Synchronized fun Player.spectateGame(game: Game): CompletableFuture<Boolean> {
        if (!game.gameTypeInfo.spectatable) return CompletableFuture.completedFuture(false)
        if (game.spectators.contains(this) || hasTag(joiningGameTag)) {
            sendMessage(Component.text("Already spectating this game", NamedTextColor.RED))
            return CompletableFuture.completedFuture(false)
        }

        val lastGame = this.game
        sendMessage(Component.text("Spectating ${game.gameTypeInfo.gameName}...", NamedTextColor.GREEN))

        setTag(joiningGameTag, 1)

        val future = game.addSpectator(this)
        handleJoin(this, lastGame, game, future)

        return future
    }

    @Synchronized fun Player.joinGame(game: Game): CompletableFuture<Boolean> {
        if (!game.canBeJoined(this)) return CompletableFuture.completedFuture(false)

        logger.info("Joining game ${game.gameTypeInfo.gameName}")

        val lastGame = this.game
        if (lastGame != null) sendMessage(Component.text("Joining ${game.gameTypeInfo.gameName}...", NamedTextColor.GREEN))

        if (hasTag(joiningGameTag)) {
            sendMessage(Component.text("You are joining games too quickly", NamedTextColor.RED))
            return CompletableFuture.completedFuture(false)
        }
        setTag(joiningGameTag, 1)

        val future = game.addPlayer(this)
        handleJoin(this, lastGame, game, future)

        return future
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.gameOptions
    ): CompletableFuture<Boolean> = this.joinGame(findOrCreateGame(this, gameTypeName, options))

    fun findOrCreateGame(
        player: Player,
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.gameOptions
    ): Game {
        val game = gameMap[gameTypeName]?.firstOrNull {
            it.canBeJoined(player) && it.gameOptions == options
        }
            ?: createGame(gameTypeName, options, player)

        return game
    }

    fun createGame(gameTypeName: String, options: GameOptions, creator: Player? = null): Game {
        nextGameID++

        //options.private = creator?.party?.privateGames ?: false

        val gameClass = gameNameToClassMap[gameTypeName]
        val game = gameClass?.primaryConstructor?.call(options)
            ?: throw IllegalArgumentException("Primary constructor not found.")
        game.gameCreator = creator

        gameMap[gameTypeName]!!.add(game)

        return game
    }

    inline fun <reified T : Game> registerGame(
        gameName: String,
        sidebarTitle: String,
        showsInSlashPlay: Boolean = true,
        canSpectate: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        gameOptions: GameOptions
    ) {
        gameNameToClassMap[gameName] = T::class

        registeredGameMap[T::class] = GameConfig(
            gameName,
            ImmortalExtension.gameConfig.serverName,
            sidebarTitle,
            showsInSlashPlay,
            canSpectate,
            whenToRegisterEvents,
            gameOptions
        )

        gameMap[gameName] = mutableSetOf()

        RedisStorage.pool.sadd("registeredGameTypes", gameName)
        RedisStorage.pool.set("${gameName}-serverName", ImmortalExtension.gameConfig.serverName)

        logger.info("Registered game type '${gameName}'")
    }
}