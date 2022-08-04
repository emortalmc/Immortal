package dev.emortal.immortal.util

import dev.emortal.immortal.game.GameManager
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.Batch
import world.cepi.kstom.Manager
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture

fun Batch<Runnable>.apply(instance: Instance) = apply(instance, null)
fun Batch<Runnable>.apply(instance: WeakReference<Instance>, callback: Runnable? = null) = instance.get()?.let { apply(it, callback) }

fun Entity.setInstance(instance: WeakReference<Instance>, pos: Pos = this.position) = instance.get()?.let { setInstance(it, pos) }
fun Entity.setInstance(instance: WeakReference<Instance>, pos: Point = this.position) = instance.get()?.let { setInstance(it, pos) }

fun Player.safeSetInstance(instance: Instance, pos: Pos? = null): CompletableFuture<Void> {
    if (!instance.isRegistered) {
        Manager.instance.registerInstance(instance)
    }

    var position = pos

    if (hasTag(GameManager.doNotTeleportTag) && isActive) {
        respawnPoint = this.position
        position = this.position
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
    return if (position == null) setInstance(instance) else setInstance(instance, position)
}

fun Player.safeSetInstance(instance: WeakReference<Instance>, pos: Pos? = null)
    = instance.get()?.let { safeSetInstance(it, pos) }