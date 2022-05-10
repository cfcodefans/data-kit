package org.h2.value

import org.h2.api.ErrorCode
import org.h2.api.IntervalQualifier
import org.h2.engine.Constants
import org.h2.engine.Constants.MAX_NUMERIC_PRECISION
import org.h2.engine.Constants.MAX_STRING_LENGTH
import org.h2.message.DbException
import org.h2.value.Value.Companion.NULL
import org.h2.value.Value.Companion.UNKNOWN
import kotlin.math.max
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
         * @param type the value type
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
        fun getTypeInfo(type: Int, precision: Long, scale: Int, extTypeInfo: ExtTypeInfo?): TypeInfo {
            var precision = precision
            var scale = scale

            when (type) {
                NULL, Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER, Value.BIGINT, Value.DATE, Value.UUID -> return TYPE_INFOS_BY_VALUE_TYPE[type]!!
                UNKNOWN -> return TYPE_UNKNOWN
                Value.CHAR -> return if (precision < 1) TYPE_CHAR else TypeInfo(Value.CHAR, precision.coerceAtMost(MAX_STRING_LENGTH.toLong()))
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
                Value.DOUBLE -> return if (precision == 0L || precision >= 25 && precision <= 53)
                    TypeInfo(Value.DOUBLE, precision, -1, extTypeInfo)
                else TypeInfo.TYPE_DOUBLE

                Value.DECFLOAT -> {
                    if (precision >= MAX_NUMERIC_PRECISION) return TypeInfo.TYPE_DECFLOAT
                    return TypeInfo(Value.DECFLOAT, if (precision < 1) -1L else precision, -1, null)
                }
                Value.TIME -> {
                    if (scale >= ValueTime.MAXIMUM_SCALE) return TypeInfo.TYPE_TIME
                    return TypeInfo(Value.TIME, (if (scale < 0) -1 else scale).toLong())
                }
                Value.TIME_TZ -> {
                    if (scale >= ValueTime.MAXIMUM_SCALE) return TYPE_TIME_TZ
                    return TypeInfo(Value.TIME_TZ, (if (scale < 0) -1 else scale).toLong())
                }
                Value.TIMESTAMP -> {
                    if (scale >= ValueTimestamp.MAXIMUM_SCALE) return TYPE_TIMESTAMP
                    return TypeInfo(Value.TIMESTAMP, (if (scale < 0) -1 else scale).toLong())
                }
                Value.TIMESTAMP_TZ -> {
                    if (scale >= ValueTimestamp.MAXIMUM_SCALE) return TYPE_TIMESTAMP_TZ
                    return TypeInfo(Value.TIMESTAMP_TZ, (if (scale < 0) -1 else scale).toLong())
                }
                Value.INTERVAL_YEAR,
                Value.INTERVAL_MONTH,
                Value.INTERVAL_DAY,
                Value.INTERVAL_HOUR,
                Value.INTERVAL_MINUTE,
                Value.INTERVAL_YEAR_TO_MONTH,
                Value.INTERVAL_DAY_TO_HOUR,
                Value.INTERVAL_DAY_TO_MINUTE,
                Value.INTERVAL_HOUR_TO_MINUTE -> {
                    return TypeInfo(type, if (precision < 1) -1L else if (precision > ValueInterval.MAXIMUM_PRECISION) ValueInterval.MAXIMUM_PRECISION.toLong() else precision)
                }
                Value.INTERVAL_SECOND, Value.INTERVAL_DAY_TO_SECOND, Value.INTERVAL_HOUR_TO_SECOND, Value.INTERVAL_MINUTE_TO_SECOND -> {
                    return TypeInfo(type,
                                    if (precision < 1) -1L else if (precision > ValueInterval.MAXIMUM_PRECISION) ValueInterval.MAXIMUM_PRECISION.toLong() else precision,
                                    if (scale < 0) -1 else if (scale > ValueInterval.MAXIMUM_SCALE) ValueInterval.MAXIMUM_SCALE else scale,
                                    null)
                }
                Value.JAVA_OBJECT -> {
                    if (precision < 1) return TypeInfo.TYPE_JAVA_OBJECT
                    return TypeInfo(Value.JAVA_OBJECT, precision.coerceAtMost(MAX_STRING_LENGTH.toLong()))
                }
                Value.ENUM -> return if (extTypeInfo is ExtTypeInfoEnum) extTypeInfo.type else TypeInfo.TYPE_ENUM_UNDEFINED
                Value.GEOMETRY -> return if (extTypeInfo is ExtTypeInfoGeometry) TypeInfo(Value.GEOMETRY, -1L, -1, extTypeInfo) else TypeInfo.TYPE_GEOMETRY
                Value.JSON -> {
                    if (precision < 1) return TypeInfo.TYPE_JSON
                    return TypeInfo(Value.JSON, precision.coerceAtMost(MAX_STRING_LENGTH.toLong()))
                }
                Value.ARRAY -> {
                    require(extTypeInfo is TypeInfo)
                    return TypeInfo(Value.ARRAY,
                                    if (precision < 0 || precision >= Constants.MAX_ARRAY_CARDINALITY) -1 else precision,
                                    -1,
                                    extTypeInfo)
                }
                Value.ROW -> {
                    require(extTypeInfo is ExtTypeInfoRow)
                    return TypeInfo(Value.ROW, -1L, -1, extTypeInfo)
                }
                else -> return TYPE_NULL
            }
        }


        /**
         * Get the higher data type of all values.
         *
         * @param values the values
         * @return the higher data type
         */
        open fun getHigherType(values: Array<Typed?>): TypeInfo? {
            if (values.isEmpty()) return TYPE_NULL

            var type: TypeInfo? = values[0].type
            var hasUnknown = false
            var hasNull = false
            when (type.valueType) {
                UNKNOWN -> hasUnknown = true
                NULL -> hasNull = true
            }
            for (i in 1 until values.size) {
                val t: TypeInfo = values[i]!!.type!!
                when (t.valueType) {
                    UNKNOWN -> hasUnknown = true
                    NULL -> hasNull = true
                    else -> type = getHigherType(type!!, t)
                }
            }
            if (type.valueType <= NULL && hasUnknown) {
                throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, if (hasNull) "NULL, ?" else "?")
            }
            return type
        }

        private fun getHigherArray(type1: TypeInfo, type2: TypeInfo, d1: Int, d2: Int): TypeInfo? {
            var type1 = type1
            var type2 = type2
            var d1 = d1
            var d2 = d2
            val precision: Long
            if (d1 > d2) {
                d1--
                precision = max(type1.precision, 1L)
                type1 = type1!!.extTypeInfo as TypeInfo
            } else if (d1 < d2) {
                d2--
                precision = max(1L, type2.precision)
                type2 = type2!!.extTypeInfo as TypeInfo
            } else if (d1 > 0) {
                d1--
                d2--
                precision = max(type1.precision, type2.precision)
                type1 = type1!!.extTypeInfo as TypeInfo
                type2 = type2!!.extTypeInfo as TypeInfo
            } else {
                return getHigherType(type1!!, type2!!)
            }
            return getTypeInfo(Value.ARRAY, precision, 0, getHigherArray(type1, type2, d1, d2))
        }

        private fun dimensions(type: TypeInfo): Int {
            var type: TypeInfo = type
            var result: Int = 0
            while (type.valueType == Value.ARRAY) {
                type = type.extTypeInfo as TypeInfo
                result++
            }
            return result
        }

        private fun getHigherRow(type1: TypeInfo, type2: TypeInfo): TypeInfo? {
            var type1: TypeInfo = type1
            var type2: TypeInfo = type2
            if (type1.valueType != Value.ROW) {
                type1 = typeToRow(type1)
            }
            if (type2.valueType != Value.ROW) {
                type2 = typeToRow(type2)
            }
            val ext1 = type1.extTypeInfo as ExtTypeInfoRow
            val ext2 = type2.extTypeInfo as ExtTypeInfoRow
            if (ext1 == ext2) return type1

            val m1 = ext1.getFields()
            val m2 = ext2.getFields()
            val degree = m1.size
            if (m2.size != degree) {
                throw DbException.get(ErrorCode.COLUMN_COUNT_DOES_NOT_MATCH)
            }
            val m = LinkedHashMap<String, TypeInfo>(Math.ceil(degree / .75).toInt())
            val i1 = m1.iterator()
            val i2 = m2.iterator()
            while (i1.hasNext()) {
                val (key, value) = i1.next()
                m[key] = getHigherType(value, i2.next().value)
            }
            return getTypeInfo(Value.ROW, 0, 0, ExtTypeInfoRow(m))
        }

        private fun typeToRow(type: TypeInfo): TypeInfo {
            val map = LinkedHashMap<String, TypeInfo>(2)
            map["C1"] = type
            return getTypeInfo(Value.ROW, 0, 0, ExtTypeInfoRow(map))
        }

        /**
         * Get the higher data type of two data types. If values need to be
         * converted to match the other operands data type, the value with the lower
         * order is converted to the value with the higher order.
         *
         * @param type1 the first data type
         * @param type2 the second data type
         * @return the higher data type of the two
         */
        fun getHigherType(type1: TypeInfo, type2: TypeInfo): TypeInfo {
            var type1 = type1
            var type2 = type2
            var t1: Int = type1.valueType
            var t2: Int = type2.valueType
            val dataType: Int
            if (t1 == t2) {
                if (t1 == UNKNOWN) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?, ?")
                dataType = t1
            } else {
                if (t1 < t2) {
                    val t = t1
                    t1 = t2
                    t2 = t
                    val type = type1
                    type1 = type2
                    type2 = type
                }
                if (t1 == UNKNOWN) {
                    if (t2 == NULL) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?, NULL")
                    return type2
                } else if (t2 == UNKNOWN) {
                    if (t1 == NULL) throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "NULL, ?")
                    return type1
                }
                if (t2 == NULL) return type1
                dataType = Value.getHigherOrderKnown(t1, t2)
            }

            val precision: Long
            when (dataType) {
                Value.NUMERIC -> {
                    type1 = type1.toNumericType()
                    type2 = type2.toNumericType()

                    var precision1: Long = type1.precision
                    var precision2: Long = type2.precision
                    val scale1: Int = type1.scale
                    val scale2: Int = type2.scale
                    val scale: Int = if (scale1 < scale2) {
                        precision1 += (scale2 - scale1).toLong()
                        scale2
                    } else {
                        precision2 += (scale1 - scale2).toLong()
                        scale1
                    }
                    return getTypeInfo(Value.NUMERIC, max(precision1, precision2), scale, null)
                }
                Value.REAL, Value.DOUBLE -> precision = -1L
                Value.ARRAY -> return getHigherArray(type1, type2, dimensions(type1), dimensions(type2))
                Value.ROW -> return TypeInfo.getHigherRow(type1, type2)
                else -> precision = Math.max(type1.precision, type2.precision)
            }
            val ext1 = type1.extTypeInfo
            return getTypeInfo(dataType,  //
                               precision,  //
                               max(type1.scale, type2.scale),  //
                               if (dataType == t1 && ext1 != null) ext1 else if (dataType == t2) type2.extTypeInfo else null)
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

    /**
     * Convert this type information to compatible NUMERIC type information.
     *
     * @return NUMERIC type information
     */
    open fun toNumericType(): TypeInfo? {
        return when (valueType) {
            Value.BOOLEAN, Value.TINYINT, Value.SMALLINT, Value.INTEGER -> getTypeInfo(Value.NUMERIC, getDecimalPrecision(), 0, null)
            Value.BIGINT -> TYPE_NUMERIC_BIGINT
            Value.NUMERIC -> this
            Value.REAL ->             // Smallest REAL value is 1.4E-45 with precision 2 and scale 46
                // Largest REAL value is 3.4028235E+38 with precision 8 and scale
                // -31
                getTypeInfo(Value.NUMERIC, 85, 46, null)
            Value.DOUBLE ->             // Smallest DOUBLE value is 4.9E-324 with precision 2 and scale 325
                // Largest DOUBLE value is 1.7976931348623157E+308 with precision 17
                // and scale -292
                getTypeInfo(Value.NUMERIC, 634, 325, null)
            else -> TYPE_NUMERIC_FLOATING_POINT
        }
    }

    /**
     * Returns approximate precision in decimal digits for binary numeric data
     * types and precision for all other types.
     *
     * @return precision in decimal digits
     */
    open fun getDecimalPrecision(): Long = when (valueType) {
        Value.TINYINT -> ValueTinyint.DECIMAL_PRECISION.toLong()
        Value.SMALLINT -> ValueSmallint.DECIMAL_PRECISION.toLong()
        Value.INTEGER -> ValueInteger.DECIMAL_PRECISION.toLong()
        Value.BIGINT -> ValueBigint.DECIMAL_PRECISION.toLong()
        Value.REAL -> ValueReal.DECIMAL_PRECISION.toLong()
        Value.DOUBLE -> ValueDouble.DECIMAL_PRECISION.toLong()
        else -> precision
    }
}