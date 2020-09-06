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

        private lateinit var TYPE_INFOS_BY_VALUE_TYPE: Array<TypeInfo>

        /**
         * Get the data type with parameters object for the given value type and the
         * specified parameters.
         * @param type the value type
         * @param precision the precision
         * @param scale the scale
         * @param extTypeInfo the extended type information, or null
         * @return the data type with parameters object
         */
        @JvmStatic
        fun getTypeInfo(type: Int, precision: Long, scale: Int, extTypeInfo: ExtTypeInfo): TypeInfo {
            when (type) {
                Value.NULL,
                Value.BOOLEAN,
                Value.BYTE,
                Value.SHORT,
                Value.INT,
                Value.LONG,
                Value.DOUBLE,
                Value.FLOAT,
                Value.DATE,
                Value.RESULT_SET,
                Value.JAVA_OBJECT,
                Value.UUID,
                Value.ROW,
                Value.JSON -> TYPE_INFOS_BY_VALUE_TYPE[type]

                Value.UNKNOWN -> TYPE_UNKNOWN

                Value.DECIMAL -> {
                    val _scale = if (scale < 0) ValueDecimal.DEFAULT_SCALE else scale
                    TypeInfo(valueType = Value.DECIMAL,
                            precision = if (precision < 0) Value)
                }
            }
        }
    }
}