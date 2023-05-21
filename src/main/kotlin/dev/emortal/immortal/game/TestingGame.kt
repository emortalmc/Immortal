package dev.emortal.immortal.game

import dev.emortal.immortal.debug.TestingGame
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class TestingGame : Game() {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(TestingGame::class.java)
    }

    override val maxPlayers: Int = 14
    override val minPlayers: Int = 2
    override val countdownSeconds: Int = 5
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = true
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = true

    override val gameName: String = "TestingGame"
    override val gameComponent: Component = Component.text(gameName)

    override fun playerJoin(player: Player) {

    }

    override fun playerLeave(player: Player) {

    }

    override fun gameStarted() {
        LOGGER.info("Testing game $id started")
    }

    override fun gameEnded() {
        LOGGER.info("Testing game $id ended")
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        LOGGER.info("Testing game $id register events")
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
//        val future = CompletableFuture<Instance>()
        val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()

        LOGGER.info("Creating instance. loadin")

        newInstance.enableAutoChunkLoad(false)

        return CompletableFuture.completedFuture(newInstance)
    }

}