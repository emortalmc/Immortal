package dev.emortal.immortal.pool

import org.jetbrains.annotations.ApiStatus
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.function.Supplier
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

@ApiStatus.Internal
    class DynamicPoolImpl<V>(private val supplier: Supplier<V>, private val usedObjectsCount: Supplier<Int>) : Pool<V> {
        private val pool: Queue<V> = ArrayDeque()

        @Volatile
        private var resizing: CompletableFuture<Void>? = null
        override fun acquire(): CompletableFuture<V> {
            synchronized(pool) {
                return if (pool.isEmpty()) {
                    resize()!!.thenCompose { acquire() }
                } else {
                    CompletableFuture.completedFuture(pool.poll()).thenApply { obj: V ->
                        resize()
                        obj
                    }
                }
            }
        }

        private fun resize(): CompletableFuture<Void>? {
            if (resizing != null) {
                return resizing!!.thenRun {
                    resizing = null
                    resize()
                }
            }
            resizing = CompletableFuture.runAsync({
                synchronized(pool) {
                    val usedObjects = usedObjectsCount.get()
                    val log2 = ceil(ln(usedObjects.toDouble()) / ln(2.0)).toInt()
                    var targetSize = 2.0.pow(log2.toDouble()).toInt()
                    targetSize = targetSize.coerceAtLeast(1)
                    while (pool.size < targetSize) {
                        pool.add(supplier.get())
                    }
                    while (pool.size > targetSize) {
                        pool.poll()
                    }
                }
            }, ForkJoinPool.commonPool())
            return resizing
        }
    }