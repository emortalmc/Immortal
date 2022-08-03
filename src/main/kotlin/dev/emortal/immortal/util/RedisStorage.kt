package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.redisson.Redisson
import org.redisson.config.Config
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.util.*


object RedisStorage {

    val redisson =
        if (System.getProperty("debug") != "true") {
            Redisson.create(
                Config().also {
                    it.useSingleServer()
                        .setAddress(ImmortalExtension.gameConfig.address)
                        .setClientName(ImmortalExtension.gameConfig.serverName)
                        .setConnectionPoolSize(5)
                }
            )
        } else null

    val registerTopic = redisson?.getTopic("registergame")
    val playerCountTopic = redisson?.getTopic("playercount")
    val joinGameTopic = redisson?.getTopic("joingame")

    fun init() {
        if (redisson == null) return

        // Create proxyhello listener
        redisson.getTopic("proxyhello")?.addListenerAsync(String::class.java) { _, _ ->
            GameManager.gameMap.keys.forEach {
                Logger.info("Received proxyhello, re-registering game $it")
                registerTopic?.publishAsync("$it ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")
            }
        }

        // Create changegame listener
        redisson.getTopic("playerpubsub${ImmortalExtension.gameConfig.serverName}")?.addListenerAsync(String::class.java) { _, msg ->
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
    }

}