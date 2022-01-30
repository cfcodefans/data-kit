package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.IntervalQualifier
import org.h2.engine.Constants
import org.h2.engine.Constants.MAX_NUMERIC_PRECISION
import org.h2.engine.Constants.MAX_STRING_LENGTH
import org.h2.message.DbException
import org.h2.util.Typed
import org.h2.value.Value.Companion.NULL
import org.h2.value.Value.Companion.UNKNOWN
import kotlin.math.min

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

    override val type: TypeInfo?
        get() = this

    companion object {

        private var TYPE_INFOS_BY_VALUE_TYPE: Array<TypeInfo?> = arrayOfNulls(Value.TYPE_COUNT)

        init {
            // INTERVAL
            for (i in Value.INTERVAL_YEAR..Value.INTERVAL_MINUTE_TO_SECOND) {
                TYPE_INFOS_BY_VALUE_TYPE[i] = TypeInfo(valueType = i,
                        precision = ValueInterval.MAXIMUM_PRECISION.toLong(),
                        scale = if (IntervalQualifier.valueOf(i - Value.INTERVAL_YEAR).hasSeconds()) ValueInterval.MAXIMUM_SCALE else -1,
                        extTypeInfo = null)
            }
        }

        /**
         * UNKNOWN type with parameters.
         */
        val TYPE_UNKNOWN: TypeInfo = TypeInfo(UNKNOWN).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        /**
         * NULL type with parameters.
         */
        val TYPE_NULL: TypeInfo = TypeInfo(NULL).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        /**
         * CHAR type with default parameters.
         */
        val TYPE_CHAR: TypeInfo = TypeInfo(Value.CHAR, -1L).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        /**
         * CHARACTER VARYING type with maximum parameters.
         */
        val TYPE_VARCHAR: TypeInfo = TypeInfo(Value.VARCHAR).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        val TYPE_CLOB: TypeInfo = TypeInfo(Value.CLOB).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_VARCHAR_IGNORECASE: TypeInfo = TypeInfo(Value.VARCHAR_IGNORECASE).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        // BINARY
        val TYPE_BINARY: TypeInfo = TypeInfo(Value.BINARY, -1L).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_VARBINARY: TypeInfo = TypeInfo(Value.VARBINARY).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_BLOB: TypeInfo = TypeInfo(Value.BLOB).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        // BOOLEAN
        val TYPE_BOOLEAN = TypeInfo(Value.BOOLEAN).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        // NUMERIC
        val TYPE_TINYINT: TypeInfo = TypeInfo(Value.TINYINT).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_SMALLINT: TypeInfo = TypeInfo(Value.SMALLINT).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_INTEGER: TypeInfo = TypeInfo(Value.INTEGER).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_BIGINT: TypeInfo = TypeInfo(Value.BIGINT).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_NUMERIC_SCALE_0: TypeInfo = TypeInfo(Value.NUMERIC, Constants.MAX_NUMERIC_PRECISION.toLong(), 0, null).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_NUMERIC_BIGINT: TypeInfo = TypeInfo(Value.NUMERIC, ValueBigint.DECIMAL_PRECISION.toLong(), 0, null).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_NUMERIC_FLOATING_POINT: TypeInfo = TypeInfo(valueType = Value.NUMERIC,
                precision = Constants.MAX_NUMERIC_PRECISION.toLong(),
                scale = Constants.MAX_NUMERIC_PRECISION / 2,
                extTypeInfo = null).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        val TYPE_REAL: TypeInfo = TypeInfo(Value.REAL).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_DOUBLE: TypeInfo = TypeInfo(Value.DOUBLE).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_DECFLOAT: TypeInfo = TypeInfo(Value.DECFLOAT).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_DECFLOAT_BIGINT: TypeInfo = TypeInfo(Value.DECFLOAT, ValueBigint.DECIMAL_PRECISION.toLong()).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        // DATETIME
        val TYPE_DATE: TypeInfo = TypeInfo(Value.DATE).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_TIME: TypeInfo = TypeInfo(Value.TIME, ValueTime.MAXIMUM_SCALE.toLong()).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_TIME_TZ: TypeInfo = TypeInfo(Value.TIME_TZ, ValueTime.MAXIMUM_SCALE.toLong()).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_TIMESTAMP: TypeInfo = TypeInfo(Value.TIMESTAMP, ValueTimestamp.MAXIMUM_SCALE.toLong()).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_TIMESTAMP_TZ: TypeInfo = TypeInfo(Value.TIMESTAMP_TZ, ValueTimestamp.MAXIMUM_SCALE.toLong()).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        val TYPE_INTERVAL_DAY: TypeInfo = TYPE_INFOS_BY_VALUE_TYPE[Value.INTERVAL_DAY]!!
        val TYPE_INTERVAL_YEAR_TO_MONTH: TypeInfo = TYPE_INFOS_BY_VALUE_TYPE[Value.INTERVAL_YEAR_TO_MONTH]!!
        val TYPE_INTERVAL_DAY_TO_SECOND: TypeInfo = TYPE_INFOS_BY_VALUE_TYPE[Value.INTERVAL_DAY_TO_SECOND]!!
        val TYPE_INTERVAL_HOUR_TO_SECOND: TypeInfo = TYPE_INFOS_BY_VALUE_TYPE[Value.INTERVAL_HOUR_TO_SECOND]!!

        // OTHER
        val TYPE_JAVA_OBJECT: TypeInfo = TypeInfo(Value.JAVA_OBJECT).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_ENUM_UNDEFINED: TypeInfo = TypeInfo(Value.ENUM).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_GEOMETRY: TypeInfo = TypeInfo(Value.GEOMETRY).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_JSON: TypeInfo = TypeInfo(Value.JSON).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_UUID: TypeInfo = TypeInfo(Value.UUID).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        // COLLECTION
        val TYPE_ARRAY_UNKNOWN: TypeInfo = TypeInfo(Value.ARRAY).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }
        val TYPE_ROW_EMPTY: TypeInfo = TypeInfo(valueType = Value.ROW,
                precision = -1L,
                scale = -1,
                extTypeInfo = ExtTypeInfoRow(LinkedHashMap())).apply { TYPE_INFOS_BY_VALUE_TYPE[valueType] = this }

        /**
         * Get the data type with parameters object for the given value type and
         * maximum parameters.
         *
         * @param type
         * the value type
         * @return the data type with parameters object
         */
        fun getTypeInfo(type: Int): TypeInfo {
            if (type == UNKNOWN) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?")
            if (type >= NULL && type < Value.TYPE_COUNT) {
                val t = TYPE_INFOS_BY_VALUE_TYPE[type]
                if (t != null) return t
            }
            return TYPE_NULL
        }

        /**
         * Get the data type with parameters object for the given value type and the
         * specified parameters.
         *
         * @param type the value type
         * @param precision the precision or `-1L` for default
         * @param scale the scale or `-1` for default
         * @param extTypeInfo the extended type information or null
         * @return the data type with parameters object
         */
        fun getTypeInfo(type: Int, precision: Long, scale: Int, extTypeInfo: ExtTypeInfo?): TypeInfo? {
            var precision = precision
            var scale = scale

            when (type) {
                NULL, Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DATE, Value.UUID -> return TYPE_INFOS_BY_VALUE_TYPE[type]
                UNKNOWN -> return TYPE_UNKNOWN
                Value.CHAR -> {
                    return if (precision < 1) TYPE_CHAR else TypeInfo(Value.CHAR, precision.coerceAtMost(MAX_STRING_LENGTH.toLong()))
                }
                Value.VARCHAR -> {
                    if (precision < 1 || precision >= MAX_STRING_LENGTH) {
                        if (precision != 0L) return TYPE_VARCHAR
                        precision = 1
                    }
                    return TypeInfo(Value.VARCHAR, precision)
                }
                Value.CLOB -> return if (precision < 1) TYPE_CLOB else TypeInfo(Value.CLOB, precision)
                Value.VARCHAR_IGNORECASE -> {
                    if (precision < 1 || precision >= MAX_STRING_LENGTH) {
                        if (precision != 0L) return TYPE_VARCHAR_IGNORECASE
                        precision = 1
                    }
                    return TypeInfo(Value.VARCHAR_IGNORECASE, precision)
                }
                Value.BINARY -> return TypeInfo(Value.BINARY, min(precision, MAX_STRING_LENGTH.toLong()))
                Value.VARBINARY -> {
                    if (precision < 1 || precision >= MAX_STRING_LENGTH) {
                        if (precision != 0L) return TYPE_VARBINARY
                        precision = 1
                    }
                    return TypeInfo(Value.VARBINARY, precision)
                }
                Value.BLOB -> return if (precision < 1) TYPE_BLOB else TypeInfo(Value.BLOB, precision)
                Value.NUMERIC -> {
                    return TypeInfo(Value.NUMERIC,
                            if (precision < 1) -1 else if (precision > MAX_NUMERIC_PRECISION) MAX_NUMERIC_PRECISION.toLong() else precision,
                            if (scale < 0) -1 else if (scale > ValueNumeric.MAXIMUM_SCALE) ValueNumeric.MAXIMUM_SCALE else scale,
                            extTypeInfo as? ExtTypeInfoNumeric)
                }
                Value.REAL -> return if (precision in 1..24) TypeInfo(Value.REAL, precision, -1, extTypeInfo) else TypeInfo.TYPE_REAL
                Value.DOUBLE -> return if (precision == 0L || precision >= 25 && precision <= 53) {
                    TypeInfo(Value.DOUBLE, precision, -1, extTypeInfo)
                } else TypeInfo.TYPE_DOUBLE
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
                    if (precision < 1) return TypeInfo.TYPE_JSON
                    else if (precision > Constants.MAX_STRING_LENGTH) precision = Constants.MAX_STRING_LENGTH.toLong()
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
                else -> return TYPE_NULL
            }
        }
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        when (valueType) {
            Value.CHAR, Value.VARCHAR, Value.CLOB, Value.VARCHAR_IGNORECASE, Value.BINARY, Value.VARBINARY, Value.BLOB, Value.JAVA_OBJECT, Value.JSON -> {
                builder.append(Value.getTypeName(valueType))
                if (precision >= 0L) builder.append('(').append(precision).append(')')
            }
            Value.NUMERIC -> {
                if (extTypeInfo != null) {
                    extTypeInfo.getSQL(builder, sqlFlags)
                } else {
                    builder.append("NUMERIC")
                }
                val withPrecision = precision >= 0
                val withScale = scale >= 0
                if (withPrecision || withScale) {
                    builder.append('(').append(if (withPrecision) precision else MAX_NUMERIC_PRECISION)
                    if (withScale) builder.append(", ").append(scale)
                    builder.append(')')
                }
            }
            Value.REAL, Value.DOUBLE -> if (precision < 0) {
                builder.append(Value.getTypeName(valueType))
            } else {
                builder.append("FLOAT")
                if (precision > 0) builder.append('(').append(precision).append(')')
            }
            Value.DECFLOAT -> {
                builder.append("DECFLOAT")
                if (precision >= 0) builder.append('(').append(precision).append(')')
            }
            Value.TIME, Value.TIME_TZ -> {
                builder.append("TIME")
                if (scale >= 0) builder.append('(').append(scale).append(')')
                if (valueType == Value.TIME_TZ) builder.append(" WITH TIME ZONE")
            }
            Value.TIMESTAMP, Value.TIMESTAMP_TZ -> {
                builder.append("TIMESTAMP")
                if (scale >= 0) builder.append('(').append(scale).append(')')
                if (valueType == Value.TIMESTAMP_TZ) builder.append(" WITH TIME ZONE")
            }
            Value.INTERVAL_YEAR, Value.INTERVAL_MONTH,
            Value.INTERVAL_DAY, Value.INTERVAL_HOUR,
            Value.INTERVAL_MINUTE, Value.INTERVAL_SECOND,
            Value.INTERVAL_YEAR_TO_MONTH, Value.INTERVAL_DAY_TO_HOUR,
            Value.INTERVAL_DAY_TO_MINUTE, Value.INTERVAL_DAY_TO_SECOND,
            Value.INTERVAL_HOUR_TO_MINUTE, Value.INTERVAL_HOUR_TO_SECOND,
            Value.INTERVAL_MINUTE_TO_SECOND -> IntervalQualifier
                    .valueOf(valueType - Value.INTERVAL_YEAR)
                    .getTypeName(builder, precision.toInt(), scale, false)
            Value.ENUM -> extTypeInfo!!.getSQL(builder.append("ENUM"), sqlFlags)
            Value.GEOMETRY -> {
                builder.append("GEOMETRY")
                extTypeInfo?.getSQL(builder, sqlFlags)
            }
            Value.ARRAY -> {
                extTypeInfo?.getSQL(builder, sqlFlags)?.append(' ')
                builder.append("ARRAY")
                if (precision >= 0L) builder.append('[').append(precision).append(']')
            }
            Value.ROW -> {
                builder.append("ROW")
                extTypeInfo?.getSQL(builder, sqlFlags)
            }
            else -> builder.append(Value.getTypeName(valueType))
        }
        return builder
    }

    /**
     * Returns the precision, or `-1L` if not specified in data type definition.
     *
     * @return the precision, or `-1L` if not specified in data type definition
     */
    open fun getDeclaredPrecision(): Long = precision
}