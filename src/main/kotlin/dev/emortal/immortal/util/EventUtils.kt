package dev.emortal.immortal.util

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.trait.CancellableEvent

inline fun <reified E> EventNode<in E>.cancel() where E : Event, E : CancellableEvent =
    this.addListener(E::class.java) {
        it.isCancelled = true
    }