package dev.emortal.immortal.util

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.batch.Batch
import net.minestom.server.instance.block.Block
import world.cepi.kstom.Manager
import java.lang.ref.WeakReference
import java.util.concurrent.CompletableFuture


inline fun <R> WeakReference<Instance>.ifPresent(callback: (Instance) -> R): R? = get()?.let { callback(it) }

fun Batch<Runnable>.apply(instance: Instance) = apply(instance, null)
fun Batch<Runnable>.apply(instance: WeakReference<Instance>, callback: Runnable? = null) = instance.ifPresent { apply(it, callback) }

fun Entity.setInstance(instance: WeakReference<Instance>, pos: Pos = this.position) = instance.ifPresent { setInstance(it, pos) }
fun Entity.setInstance(instance: WeakReference<Instance>, pos: Point = this.position) = instance.ifPresent { setInstance(it, pos) }

fun WeakReference<Instance>.getBlock(x: Int, y: Int, z: Int) = get()?.getBlock(x, y, z)
fun WeakReference<Instance>.getBlock(point: Point) = get()?.getBlock(point)
fun WeakReference<Instance>.setBlock(x: Int, y: Int, z: Int, block: Block) {
    get()?.setBlock(x, y, z, block)
}
fun WeakReference<Instance>.setBlock(point: Point, block: Block) {
    get()?.setBlock(point, block)
}

fun Player.safeSetInstance(instance: WeakReference<Instance>, pos: Pos? = null) = instance.ifPresent { safeSetInstance(it, pos) }

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
        if (pos == null) return CompletableFuture.completedFuture(null)
        return this.teleport(pos)
    }
    return if (pos == null) setInstance(instance) else setInstance(instance, pos)
}