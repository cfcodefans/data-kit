package org.h2.value

import org.h2.util.StringUtils
import java.util.concurrent.ConcurrentHashMap

/**
 * A concurrent hash map with case-insensitive string keys.
 *
 * @param <V> the value type
</V> */
open class CaseInsensitiveConcurrentMap<V> : ConcurrentHashMap<String, V>() {
    override operator fun get(key: String): V? {
        return super.get(StringUtils.toUpperEnglish((key as String)))
    }

    override fun put(key: String, value: V): V? {
        return super.put(StringUtils.toUpperEnglish(key), value)
    }

    override fun putIfAbsent(key: String, value: V): V? {
        return super.putIfAbsent(StringUtils.toUpperEnglish(key), value)
    }

    override fun containsKey(key: String): Boolean {
        return super.containsKey(StringUtils.toUpperEnglish((key as String)))
    }

    override fun remove(key: String): V? {
        return super.remove(StringUtils.toUpperEnglish((key as String)))
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}