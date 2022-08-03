package dev.emortal.immortal.util

import dev.emortal.immortal.ImmortalExtension
import dev.emortal.immortal.game.GameManager
import dev.emortal.immortal.game.GameManager.game
import dev.emortal.immortal.game.GameManager.joinGame
import dev.emortal.immortal.game.GameManager.joinGameOrNew
import io.github.crackthecodeabhi.kreds.connection.AbstractKredsSubscriber
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.KredsClient
import io.github.crackthecodeabhi.kreds.connection.newClient
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.tinylog.kotlin.Logger
import world.cepi.kstom.Manager
import java.util.*


object KredsStorage {

    var kreds: KredsClient? = null

//    val registerTopic = redisson?.getTopic("registergame")
//    val playerCountTopic = redisson?.getTopic("playercount")

    fun init(): Unit = runBlocking {
        launch {
            kreds = newClient(Endpoint.from(ImmortalExtension.gameConfig.address))

            val subscriptionHandler = object : AbstractKredsSubscriber() {

                override fun onMessage(channel: String, message: String): Unit = runBlocking {
                    coroutineScope {
                        when (channel) {
                            "proxyhello" -> {
                                GameManager.gameMap.keys.forEach {
                                    Logger.info("Received proxyhello, re-registering game $it")
                                    kreds?.publish("registergame", "$it ${ImmortalExtension.gameConfig.serverName} ${ImmortalExtension.gameConfig.serverPort}")
                                }
                            }

                            "playerpubsub" -> {
                                val args = message.split(" ")

                                when (args[0].lowercase()) {
                                    "changegame" -> {
                                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return@coroutineScope
                                        val subgame = args[2]
                                        if (!GameManager.gameMap.containsKey(subgame)) {
                                            // Invalid subgame, ignore message
                                            Logger.warn("Invalid subgame $subgame")
                                            return@coroutineScope
                                        }

                                        if (player.game?.gameName == subgame) return@coroutineScope

                                        player.scheduleNextTick {
                                            player.joinGameOrNew(subgame)
                                        }
                                    }

                                    "spectateplayer" -> {
                                        val player = Manager.connection.getPlayer((UUID.fromString(args[1]))) ?: return@coroutineScope
                                        val playerToSpectate = Manager.connection.getPlayer((UUID.fromString(args[2]))) ?: return@coroutineScope

                                        val game = playerToSpectate.game
                                        if (game != null) {
                                            if (!game.gameOptions.allowsSpectators) {
                                                player.sendMessage(Component.text("That game does not allow spectating", NamedTextColor.RED))
                                                return@coroutineScope
                                            }
                                            if (game.id == player.game?.id) {
                                                player.sendMessage(Component.text("That player is not on a game", NamedTextColor.RED))
                                                return@coroutineScope
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

                }

                override fun onException(ex: Throwable) {
                    Manager.exception.handleException(ex)
                }

            }
        }

    }

}
