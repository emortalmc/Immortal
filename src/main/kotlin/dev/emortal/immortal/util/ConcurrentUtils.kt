package dev.emortal.immortal.util

import java.util.concurrent.CompletableFuture

import java.util.concurrent.CountDownLatch


fun CountDownLatch.toFuture(): CompletableFuture<Void?> {
    val future = CompletableFuture<Void?>()
    CompletableFuture.runAsync {
        try {
            this.await()
            future.complete(null)
        } catch (e: InterruptedException) {
            future.completeExceptionally(e)
        }
    }
    return future
}