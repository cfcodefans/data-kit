package org.h2.value

import org.h2.engine.Constants
import org.h2.value.Value.Companion.NULL
import org.h2.value.Value.Companion.UNKNOWN

/**
 * Data type with parameters.
 *
 * Create new instance of data type with parameters.
 * @param valueType the value type
 * @param precision the precision
 * @param scale the scale
 * @param extTypeInfo the extended type information, or null
 */
open class TypeInfo(
        val valueType: Int,
        val precision: Long = -1L,
        val scale: Int = -1,
        val extTypeInfo: ExtTypeInfo? = null) : ExtTypeInfo(), Typed {


    companion object {
        /**
         * UNKNOWN type with parameters.
         */
        val TYPE_UNKNOWN: TypeInfo = TypeInfo(UNKNOWN)

        /**
         * NULL type with parameters.
         */
        val TYPE_NULL: TypeInfo = TypeInfo(NULL)

        /**
         * CHAR type with default parameters.
         */
        val TYPE_CHAR: TypeInfo? = TypeInfo(Value.CHAR, -1L)

        /**
         * CHARACTER VARYING type with maximum parameters.
         */
        val TYPE_VARCHAR: TypeInfo? = TypeInfo(Value.VARCHAR)

        private lateinit var TYPE_INFOS_BY_VALUE_TYPE: Array<TypeInfo>

        /**
         * Get the data type with parameters object for the given value type and the
         * specified parameters.
         *
         * @param type
         * the value type
         * @param precision
         * the precision or `-1L` for default
         * @param scale
         * the scale or `-1` for default
         * @param extTypeInfo
         * the extended type information or null
         * @return the data type with parameters object
         */
        fun getTypeInfo(type: Int, precision: Long, scale: Int, extTypeInfo: ExtTypeInfo?): TypeInfo? {
            var precision = precision
            var scale = scale
            when (type) {
                NULL, Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DATE, Value.UUID -> return TYPE_INFOS_BY_VALUE_TYPE[type]
                UNKNOWN -> return TYPE_UNKNOWN
                Value.CHAR -> {
                    if (precision < 1) return TYPE_CHAR
                    if (precision > Constants.MAX_STRING_LENGTH) precision = Constants.MAX_STRING_LENGTH.toLong()
                    return TypeInfo(Value.CHAR, precision)
                }
                Value.VARCHAR -> {
                    if (precision < 1 || precision >= Constants.MAX_STRING_LENGTH) {
                        if (precision != 0L) return TypeInfo.TYPE_VARCHAR
                        precision = 1
                    }
                    return TypeInfo(Value.VARCHAR, precision)
                }
                Value.CLOB -> {
                    return if (precision < 1) {
                        TypeInfo.TYPE_CLOB
                    } else TypeInfo(Value.CLOB, precision)
                }
                Value.VARCHAR_IGNORECASE -> {
                    if (precision < 1 || precision >= Constants.MAX_STRING_LENGTH) {
                        if (precision != 0L) {
                            return TypeInfo.TYPE_VARCHAR_IGNORECASE
                        }
                        precision = 1
                    }
                    return TypeInfo(Value.VARCHAR_IGNORECASE, precision)
                }
                Value.BINARY -> {
                    if (precision < 1) {
                        return TypeInfo.TYPE_BINARY
                    }
                    if (precision > Constants.MAX_STRING_LENGTH) {
                        precision = Constants.MAX_STRING_LENGTH.toLong()
                    }
                    return TypeInfo(Value.BINARY, precision)
                }
                Value.VARBINARY -> {
                    if (precision < 1 || precision >= Constants.MAX_STRING_LENGTH) {
                        if (precision != 0L) {
                            return TypeInfo.TYPE_VARBINARY
                        }
                        precision = 1
                    }
                    return TypeInfo(Value.VARBINARY, precision)
                }
                Value.BLOB -> {
                    return if (precision < 1) {
                        TypeInfo.TYPE_BLOB
                    } else TypeInfo(Value.BLOB, precision)
                }
                Value.NUMERIC -> {
                    if (precision < 1) {
                        precision = -1L
                    } else if (precision > Constants.MAX_NUMERIC_PRECISION) {
                        precision = Constants.MAX_NUMERIC_PRECISION.toLong()
                    }
                    if (scale < 0) {
                        scale = -1
                    } else if (scale > ValueNumeric.MAXIMUM_SCALE) {
                        scale = ValueNumeric.MAXIMUM_SCALE
                    }
                    return TypeInfo(Value.NUMERIC, precision, scale,
                            extTypeInfo as? ExtTypeInfoNumeric)
                }
                Value.REAL -> {
                    return if (precision in 1..24) {
                        TypeInfo(Value.REAL, precision, -1, extTypeInfo)
                    } else TypeInfo.TYPE_REAL
                }
                Value.DOUBLE -> {
                    return if (precision == 0L || precision >= 25 && precision <= 53) {
                        TypeInfo(Value.DOUBLE, precision, -1, extTypeInfo)
                    } else TypeInfo.TYPE_DOUBLE
                }
                Value.DECFLOAT -> {
                    if (precision < 1) {
                        precision = -1L
                    } else if (precision >= Constants.MAX_NUMERIC_PRECISION) {
                        return TypeInfo.TYPE_DECFLOAT
                    }
                    return TypeInfo(Value.DECFLOAT, precision, -1, null)
                }
                Value.TIME -> {
                    if (scale < 0) {
                        scale = -1
                    } else if (scale >= ValueTime.MAXIMUM_SCALE) {
                        return TypeInfo.TYPE_TIME
                    }
                    return TypeInfo(Value.TIME, scale.toLong())
                }
                Value.TIME_TZ -> {
                    if (scale < 0) {
                        scale = -1
                    } else if (scale >= ValueTime.MAXIMUM_SCALE) {
                        return TypeInfo.TYPE_TIME_TZ
                    }
                    return TypeInfo(Value.TIME_TZ, scale.toLong())
                }
                Value.TIMESTAMP -> {
                    if (scale < 0) {
                        scale = -1
                    } else if (scale >= ValueTimestamp.MAXIMUM_SCALE) {
                        return TypeInfo.TYPE_TIMESTAMP
                    }
                    return TypeInfo(Value.TIMESTAMP, scale.toLong())
                }
                Value.TIMESTAMP_TZ -> {
                    if (scale < 0) {
                        scale = -1
                    } else if (scale >= ValueTimestamp.MAXIMUM_SCALE) {
                        return TypeInfo.TYPE_TIMESTAMP_TZ
                    }
                    return TypeInfo(Value.TIMESTAMP_TZ, scale.toLong())
                }
                Value.INTERVAL_YEAR, Value.INTERVAL_MONTH, Value.INTERVAL_DAY, Value.INTERVAL_HOUR, Value.INTERVAL_MINUTE, Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR, Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_HOUR_TO_MINUTE -> {
                    if (precision < 1) {
                        precision = -1L
                    } else if (precision > ValueInterval.MAXIMUM_PRECISION) {
                        precision = ValueInterval.MAXIMUM_PRECISION.toLong()
                    }
                    return TypeInfo(type, precision)
                }
                Value.INTERVAL_SECOND, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND -> {
                    if (precision < 1) {
                        precision = -1L
                    } else if (precision > ValueInterval.MAXIMUM_PRECISION) {
                        precision = ValueInterval.MAXIMUM_PRECISION.toLong()
                    }
                    if (scale < 0) {
                        scale = -1
                    } else if (scale > ValueInterval.MAXIMUM_SCALE) {
                        scale = ValueInterval.MAXIMUM_SCALE
                    }
                    return TypeInfo(type, precision, scale, null)
                }
                Value.JAVA_OBJECT -> {
                    if (precision < 1) {
                        return TypeInfo.TYPE_JAVA_OBJECT
                    } else if (precision > Constants.MAX_STRING_LENGTH) {
                        precision = Constants.MAX_STRING_LENGTH.toLong()
                    }
                    return TypeInfo(Value.JAVA_OBJECT, precision)
                }
                Value.ENUM -> return if (extTypeInfo is ExtTypeInfoEnum) {
                    extTypeInfo.type
                } else {
                    TypeInfo.TYPE_ENUM_UNDEFINED
                }
                Value.GEOMETRY -> return if (extTypeInfo is ExtTypeInfoGeometry) {
                    TypeInfo(Value.GEOMETRY, -1L, -1, extTypeInfo)
                } else {
                    TypeInfo.TYPE_GEOMETRY
                }
                Value.JSON -> {
                    if (precision < 1) {
                        return TypeInfo.TYPE_JSON
                    } else if (precision > Constants.MAX_STRING_LENGTH) {
                        precision = Constants.MAX_STRING_LENGTH.toLong()
                    }
                    return TypeInfo(Value.JSON, precision)
                }
                Value.ARRAY -> {
                    require(extTypeInfo is TypeInfo)
                    if (precision < 0 || precision >= Constants.MAX_ARRAY_CARDINALITY) {
                        precision = -1L
                    }
                    return TypeInfo(Value.ARRAY, precision, -1, extTypeInfo)
                }
                Value.ROW -> {
                    require(extTypeInfo is ExtTypeInfoRow)
                    return TypeInfo(Value.ROW, -1L, -1, extTypeInfo)
                }
            }
            return TYPE_NULL
        }
    }
}