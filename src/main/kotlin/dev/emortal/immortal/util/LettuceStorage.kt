package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.util.*


object LettuceStorage {

    var lettuce: RedisClient? = null
    var connection: RedisAsyncCommands<String, String>? = null
    var pubSub: RedisPubSubAsyncCommands<String, String>? = null

    fun init() {
        lettuce = RedisClient.create(ImmortalExtension.gameConfig.address)
        connection = lettuce?.connect()?.async()

        val pubsubConnection = lettuce?.connectPubSub()
        pubSub = pubsubConnection?.async()

        pubsubConnection?.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                val args = message.split(" ")

                when (channel) {
                    "proxyhello" -> {
                        GameManager.gameMap.keys.forEach {
                            Logger.info("Received proxyhello, re-registering game $it")
                            pubSub?.publish("registergame", "$it ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")
                        }
                    }

                    "changegame" -> {
                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return
                        val subgame = args[2]
                        if (!GameManager.gameMap.containsKey(subgame)) {
                            // Invalid subgame, ignore message
                            Logger.warn("Invalid subgame $subgame")
                            return
                        }

                        if (player.game?.gameName == subgame) return

                        player.scheduleNextTick {
                            player.joinGameOrNew(subgame)
                        }
                    }

                    "spectateplayer" -> {
                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return
                        val playerToSpectate = Manager.connection.getPlayer((UUID.fromString(args[2]))) ?: return

                        val game = playerToSpectate.game
                        if (game != null) {
                            if (!game.gameOptions.allowsSpectators) {
                                player.sendMessage(Component.text("That game does not allow spectating", NamedTextColor.RED))
                                return
                            }
                            if (game.id == player.game?.id) {
                                player.sendMessage(Component.text("That player is not on a game", NamedTextColor.RED))
                                return
                            }

                            player.scheduleNextTick {
                                player.joinGame(game, spectate = true)
                            }
                        }
                    }
                }
            }

            override fun message(pattern: String?, channel: String?, message: String?) {}
            override fun subscribed(channel: String?, count: Long) {}
            override fun psubscribed(pattern: String?, count: Long) {}
            override fun unsubscribed(channel: String?, count: Long) {}
            override fun punsubscribed(pattern: String?, count: Long) {}

        })

        pubSub?.subscribe("proxyhello", "playerpubsub${ImmortalExtension.gameConfig.serverName}", "spectateplayer")
    }

}
