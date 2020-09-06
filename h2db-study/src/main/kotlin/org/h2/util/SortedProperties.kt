package org.h2.util

import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.util.*

/**
 * Sorted properties file.
 * This implementation requires that org.h2.store() internally calls keys().
 */
class SortedProperties : Properties() {
    companion object {
        @JvmStatic
        val serialVersionUID: Long = 1L

        /**
         * Convert a string to a map
         *
         * @param s the string
         * @return the map
         */
        @JvmStatic
        fun fromLines(s: String): SortedProperties {
            val p: SortedProperties = SortedProperties()
            for (line in s.split("\n")) {
                val idx: Int = line.indexOf('=')
                if (idx > 0) {
                    p[line.substring(0, idx)] = line.substring(idx + 1)
                }
            }
            return p
        }
    }

    @Synchronized
    override fun keys(): Enumeration<Any> {
        return Vector(super.keys.map { it.toString() }.sorted()).elements().cast()
    }
}