package org.h2.value

import org.h2.value.Value.Companion.NULL
import org.h2.value.Value.Companion.UNKNOWN

/**
 * Data type with parameters.
 *
 * Create new instance of data type with parameters.
 * @param valueType the value type
 * @param precision the precision
 * @param scale the scale
 * @param displaySize the display size in characters
 * @param extTypeInfo the extended type information, or null
 */
class TypeInfo(
        val valueType: Int,
        val precision: Long,
        val scale: Int,
        val displaySize: Int,
        val extTypeInfo: ExtTypeInfo?) {
    companion object {
        /**
         * UNKNOWN type with parameters.
         */
        val TYPE_UNKNOWN: TypeInfo = TypeInfo(UNKNOWN, -1L, -1, -1, null)
        /**
         * NULL type with parameters.
         */
        val TYPE_NULL: TypeInfo = TypeInfo(NULL, -1L, -1, -1, null)
    }
}