package dev.emortal.immortal.game

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import org.tinylog.kotlin.Logger
import java.util.concurrent.CompletableFuture

class TestingGame : Game() {

    override val maxPlayers: Int = 14
    override val minPlayers: Int = 2
    override val countdownSeconds: Int = 5
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = true
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = true

    override fun playerJoin(player: Player) {

    }

    override fun playerLeave(player: Player) {

    }

    override fun gameStarted() {
        Logger.info("Testing game $id started")
    }

    override fun gameEnded() {
        Logger.info("Testing game $id ended")
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {
        Logger.info("Testing game $id register events")
    }

    override fun instanceCreate(): CompletableFuture<Instance> {
//        val future = CompletableFuture<Instance>()
        val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()

        Logger.info("Creating instance. loadin")

        newInstance.enableAutoChunkLoad(false)

        return CompletableFuture.completedFuture(newInstance)
    }

}