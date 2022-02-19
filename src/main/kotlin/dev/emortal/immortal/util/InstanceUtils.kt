package dev.emortal.immortal.util

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import world.cepi.kstom.Manager
import java.util.*
import java.util.concurrent.CompletableFuture

fun Player.safeSetInstance(instance: Instance, pos: Pos? = null): CompletableFuture<Void> {
    if (instance == this.instance) {
        if (pos == null) return CompletableFuture.completedFuture(null)
        return this.teleport(pos)
    }
    return setInstance(instance)
}

fun Player.safeSetInstance(instance: UUID, pos: Pos? = null): CompletableFuture<Void> {
    val newInstance = Manager.instance.getInstance(instance)

    if (instance == this.instance?.uniqueId || newInstance == null) {
        if (pos == null) return CompletableFuture.completedFuture(null)
        return this.teleport(pos)
    }

    return setInstance(newInstance)
}