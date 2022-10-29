package dev.emortal.immortal.pool

import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

interface Pool<V> {
    /**
     * Acquires the next free object from the pool, or creates a new one if the pool is empty.
     * @return the next free object
     */
    fun acquire(): CompletableFuture<V>

    companion object {
        /**
         * Creates a pool that is dynamically sized upon demand.
         * <P>
         * The pool will create new objects as needed, and attempts to constantly match log2(usedObjects) to be available.
        </P> *
         * @param supplier the supplier to create new objects
         * @param usedObjectsCount the number of objects currently in use
         * @return the created pool
         * @param <V> the type of object in the pool
        </V> */
        fun <V> dynamic(supplier: Supplier<V>, usedObjectsCount: Supplier<Int>): Pool<V> {
            return DynamicPoolImpl(supplier, usedObjectsCount)
        }

        fun <V> constant(supplier: Supplier<V>): Pool<V> {
            return dynamic(supplier) { 0 }
        }
    }
}