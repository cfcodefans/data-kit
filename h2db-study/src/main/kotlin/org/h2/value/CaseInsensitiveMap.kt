package org.h2.value

import org.h2.util.StringUtils

/**
 * A hash map with a case-insensitive string key.
 *
 * @param <V> the value type
</V> */
class CaseInsensitiveMap<V> : HashMap<String, V?> {
    /**
     * Creates new instance of case-insensitive map.
     */
    constructor() {}

    /**
     * Creates new instance of case-insensitive map with specified initial
     * capacity.
     *
     * @param initialCapacity the initial capacity
     */
    constructor(initialCapacity: Int) : super(initialCapacity) {}

    override operator fun get(key: String): V? {
        return super.get(StringUtils.toUpperEnglish((key as String)))
    }

    override fun put(key: String, value: V?): V? {
        return super.put(StringUtils.toUpperEnglish(key), value)
    }

    override fun putIfAbsent(key: String, value: V?): V? {
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
