package dev.emortal.immortal.game

import dev.emortal.immortal.inventory.GameSelectorGUI
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
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
    val registeredGameMap = ConcurrentHashMap<KClass<out Game>, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    @Volatile
    internal var nextGameID = 0

    val Player.game get() = playerGameMap[this]

    fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Void>? = joinGame(game) ?: spectateGame(game)

    @Synchronized fun Player.spectateGame(game: Game): CompletableFuture<Void>? {
        if (!game.gameTypeInfo.spectatable) return null
        if (game.spectators.contains(this)) {
            sendMessage(Component.text("Already spectating this game", NamedTextColor.RED))
            return null
        }

        val lastGame = this.game

        if (hasTag(joiningGameTag)) {
            logger.info("Join on cooldown")
            sendMessage(Component.text("Already trying to spectating a game", NamedTextColor.RED))
            return null
        }
        setTag(joiningGameTag, 1)

        val future = game.addSpectator(this)
        if (future != null) {
            lastGame?.removePlayer(this)
            lastGame?.removeSpectator(this)
            playerGameMap[this] = game

            Manager.scheduler.buildTask {
                removeTag(joiningGameTag)
            }.delay(Duration.ofSeconds(1)).schedule()
        } else {
            sendMessage(Component.text("Something went wrong while spectating ${game.gameTypeInfo.gameName}", NamedTextColor.RED))
            playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

            removeTag(joiningGameTag)
        }

        return future
    }

    @Synchronized fun Player.joinGame(game: Game): CompletableFuture<Void>? {
        if (!game.canBeJoined(this)) return null

        logger.info("Joining game ${game.gameTypeInfo.gameName}")

        val lastGame = this.game

        if (hasTag(joiningGameTag)) {
            logger.info("Join on cooldown")
            sendMessage(Component.text("You are joining games too quickly", NamedTextColor.RED))
            return null
        }
        setTag(joiningGameTag, 1)

        val future = game.addPlayer(this)
        if (future != null) {
            future.thenRun {
                if (lastGame == null) logger.warn("Last game is null")
                lastGame?.removePlayer(this)
                lastGame?.removeSpectator(this)
                playerGameMap[this] = game

                Manager.scheduler.buildTask {
                    removeTag(joiningGameTag)
                }.delay(Duration.ofSeconds(1)).schedule()
            }
        } else {
            sendMessage(Component.text("Something went wrong while joining ${game.gameTypeInfo.gameName}", NamedTextColor.RED))
            playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())
            removeTag(joiningGameTag)
        }

        return future
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameNameToClassMap[gameTypeName]]!!.gameOptions
    ): CompletableFuture<Void>? = this.joinGame(findOrCreateGame(this, gameTypeName, options))

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

        gameMap.putIfAbsent(gameTypeName, mutableSetOf())
        gameMap[gameTypeName]!!.add(game)

        return game
    }

    inline fun <reified T : Game> registerGame(
        eventNode: EventNode<Event>,
        gameName: String,
        sidebarTitle: Component,
        showsInSlashPlay: Boolean = true,
        canSpectate: Boolean = true,
        whenToRegisterEvents: WhenToRegisterEvents = WhenToRegisterEvents.GAME_START,
        gameOptions: GameOptions
    ) {
        gameNameToClassMap[gameName] = T::class

        registeredGameMap[T::class] = GameTypeInfo(
            eventNode,
            gameName,
            sidebarTitle,
            showsInSlashPlay,
            canSpectate,
            whenToRegisterEvents,
            gameOptions
        )

        gameMap[gameName] = mutableSetOf()

        GameSelectorGUI.refresh()

        logger.info("Registered game type '${gameName}'")
    }

    inline fun <reified T : Game> unregisterGame() {
        val gameName = registeredGameMap[T::class]!!.gameName

        gameMap[gameName]?.forEach {
            it.destroy()
        }
        gameMap.remove(gameName)
        gameNameToClassMap.remove(gameName)
        registeredGameMap.remove(T::class)

        logger.info("Unregistered game type '${gameName}'")
    }
}