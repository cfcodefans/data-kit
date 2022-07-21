package org.h2.util

/**
 * This class implements a small LRU object cache.
 *
 * @param <K> the key
 * @param <V> the value
 */
open class SmallLRUCache<K, V>(var cacheSize: Int) : LinkedHashMap<K, V>(cacheSize, 0.75f, true) {

    companion object {
        /**
         * Create a new object with all elements of the given collection.
         * @param <K> the key type
         * @param <V> the value type
         * @param size the number of elements
         * @return the object
        </V></K> */
        fun <K, V> newInstance(size: Int): SmallLRUCache<K, V> = SmallLRUCache<K, V>(size)
    }

    open fun setMaxSize(size: Int) {
        this.cacheSize = size
    }

    override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean = super.size > cacheSize
}