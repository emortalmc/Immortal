package dev.emortal.immortal.game

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.config.GameConfig
import dev.emortal.immortal.config.GameOptions
import dev.emortal.immortal.config.GameTypeInfo
import dev.emortal.immortal.util.RedisStorage.jedisPool
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.sound.SoundEvent
import net.minestom.server.tag.Tag
import org.slf4j.LoggerFactory
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object GameManager {
    val doNotUnregisterTag = Tag.Byte("doNotUnregister")

    val gameNameTag = Tag.String("gameName")
    val gameIdTag = Tag.Integer("gameId")
    val joiningGameTag = Tag.Byte("joiningGame")

    val playerGameMap = ConcurrentHashMap<Player, Game>()
    val registeredClassMap = ConcurrentHashMap<KClass<out Game>, String>()
    val registeredGameMap = ConcurrentHashMap<String, GameTypeInfo>()
    val gameMap = ConcurrentHashMap<String, MutableSet<Game>>()

    val Player.game get() = playerGameMap[this]

    //fun Player.joinGameOrSpectate(game: Game): CompletableFuture<Boolean> = joinGame(game) ?: spectateGame(game)

    @Synchronized private fun handleJoin(player: Player, lastGame: Game?, newGame: Game): CompletableFuture<Boolean> {
        if (player.hasTag(joiningGameTag)) return CompletableFuture.completedFuture(false)
        player.setTag(joiningGameTag, 1)

        lastGame?.removePlayer(player)
        lastGame?.removeSpectator(player)

        val future = newGame.addPlayer(player)
        future.thenAcceptAsync { successful ->
            if (successful) {
                playerGameMap[player] = newGame

                Manager.scheduler.buildTask {
                    player.removeTag(joiningGameTag)
                }.delay(Duration.ofSeconds(1)).schedule()
            } else {
                player.sendMessage(Component.text("Something went wrong while joining ${newGame.gameTypeInfo.name}", NamedTextColor.RED))
                player.playSound(Sound.sound(SoundEvent.ENTITY_VILLAGER_NO, Sound.Source.MASTER, 1f, 1f), Sound.Emitter.self())

                player.removeTag(joiningGameTag)
            }
        }

        return future
    }

//    @Synchronized fun Player.spectateGame(game: Game): CompletableFuture<Boolean> {
//        if (!game.gameTypeInfo.spectatable) return CompletableFuture.completedFuture(false)
//        if (game.spectators.contains(this) || hasTag(joiningGameTag)) {
//            sendMessage(Component.text("Already spectating this game", NamedTextColor.RED))
//            return CompletableFuture.completedFuture(false)
//        }
//
//        val lastGame = this.game
//
//        val future = game.addSpectator(this)
//        handleJoin(this, lastGame, game, future)
//
//        return future
//    }

    @Synchronized fun Player.joinGame(game: Game): CompletableFuture<Boolean> {
        if (!game.canBeJoined(this)) return CompletableFuture.completedFuture(false)

        val lastGame = this.game
        return handleJoin(this, lastGame, game)
    }

    fun Player.joinGameOrNew(
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]?.options!!
    ): CompletableFuture<Boolean> = this.joinGame(findOrCreateGame(this, gameTypeName, options))

    fun findOrCreateGame(
        player: Player,
        gameTypeName: String,
        options: GameOptions = registeredGameMap[gameTypeName]?.options!!
    ): Game {
        val game = gameMap[gameTypeName]?.firstOrNull {
            it.canBeJoined(player) && it.gameOptions == options
        }
            ?: createGame(gameTypeName, options, player)

        return game
    }

    fun createGame(gameTypeName: String, options: GameOptions, creator: Player? = null): Game {

        //options.private = creator?.party?.privateGames ?: false

        val game = registeredGameMap[gameTypeName]?.clazz?.primaryConstructor?.call(options)
            ?: throw IllegalArgumentException("Primary constructor not found.")
        game.gameCreator = creator

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
    ) {
        registeredClassMap[T::class] = name
        registeredGameMap[name] = GameTypeInfo(
            T::class,
            name,
            title,
            showsInSlashPlay,
            canSpectate,
            whenToRegisterEvents,
            options
        )

        val jedis = jedisPool.resource
        //jedis.sadd("registeredGameTypes", name)
        //jedis.set("${name}-serverName", ImmortalExtension.gameConfig.serverName)
        jedis.publish("registergame", "$name ${ImmortalExtension.gameConfig.serverName} ${MinecraftServer.getServer().port}")

        gameMap[name] = ConcurrentHashMap.newKeySet()

        Logger.info("Registered game type '${name}'")
    }
}