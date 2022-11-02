package dev.emortal.immortal.debug

import dev.emortal.immortal.game.Game
import net.kyori.adventure.text.Component
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import java.util.concurrent.CompletableFuture

class TestingGame : Game() {


    override val maxPlayers: Int = 10
    override val minPlayers: Int = 2
    override val countdownSeconds: Int = 0
    override val canJoinDuringGame: Boolean = false
    override val showScoreboard: Boolean = false
    override val showsJoinLeaveMessages: Boolean = true
    override val allowsSpectators: Boolean = false

    override fun playerJoin(player: Player) {

    }

    override fun playerLeave(player: Player) {

    }

    override fun gameStarted() {
        sendMessage(Component.text("Game started!!"))
    }

    override fun gameEnded() {
        sendMessage(Component.text("Game ended!!"))
    }

    override fun registerEvents(eventNode: EventNode<InstanceEvent>) {

    }

    override fun instanceCreate(): CompletableFuture<Instance> {
        val newInstance = MinecraftServer.getInstanceManager().createInstanceContainer()
        newInstance.setGenerator {
            it.modifier().fillHeight(0, 1, Block.GRASS_BLOCK)
        }

        return CompletableFuture.completedFuture(newInstance)
    }


}