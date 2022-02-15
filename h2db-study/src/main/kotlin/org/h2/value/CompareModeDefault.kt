package org.h2.value

import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.SmallLRUCache
import java.text.CollationKey
import java.text.Collator

/**
 * The default implementation of CompareMode. It uses java.text.Collator.
 */
open class CompareModeDefault(name: String, strength: Int) : CompareMode(name, strength) {
    private lateinit var collator: Collator
    private var collationKeys: SmallLRUCache<String, CollationKey>? = null

    @Volatile
    private var caseInsensitive: CompareModeDefault? = null

    init {
        collator = getCollator(name) ?: throw DbException.getInternalError(name)
        collator.strength = strength

        val cacheSize = SysProperties.COLLATOR_CACHE_SIZE
        collationKeys = (if (cacheSize != 0) SmallLRUCache.newInstance<String, CollationKey>(cacheSize) else null)
    }

    override fun compareString(a: String, b: String?, ignoreCase: Boolean): Int {
        if (ignoreCase && strength > Collator.SECONDARY) {
            var i: CompareModeDefault? = caseInsensitive
            if (i == null) {
                i = CompareModeDefault(name!!, Collator.SECONDARY)
                caseInsensitive = i
            }
            return i.compareString(a, b, false)
        }
        return if (collationKeys != null) {
            val aKey: CollationKey = getKey(a)
            val bKey: CollationKey = getKey(b!!)
            aKey.compareTo(bKey)
        } else {
            collator.compare(a, b)
        }
    }

    private fun getKey(a: String): CollationKey {
        synchronized(collationKeys!!) {
            return collationKeys!!.computeIfAbsent(a) { collator.getCollationKey(a) }
        }
    }

    override fun equalsChars(a: String, ai: Int, b: String, bi: Int, ignoreCase: Boolean): Boolean {
        return compareString(a.substring(ai, ai + 1), b.substring(bi, bi + 1), ignoreCase) == 0
    }
}