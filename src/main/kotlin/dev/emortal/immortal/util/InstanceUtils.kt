package dev.emortal.immortal.util

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.Batch
import world.cepi.kstom.Manager
import java.util.concurrent.CompletableFuture

fun Batch<Runnable>.apply(instance: Instance) = apply(instance, null)

fun Player.safeSetInstance(instance: Instance, pos: Pos? = null): CompletableFuture<Void> {
    if (!instance.isRegistered) {
        Manager.instance.registerInstance(instance)
    }

    if (!isActive) {
        val future = CompletableFuture<Void>()

        scheduleNextTick {
            future.complete(null)
        }

        return future
    }
    if (instance == this.instance) {
        /*if (position == null)*/ return CompletableFuture.completedFuture(null)
        //return this.teleport(pos)
    }
    return if (pos == null) setInstance(instance) else setInstance(instance, pos)
}