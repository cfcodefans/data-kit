package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Mode.CharPadding
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.Bits
import org.h2.util.DateTimeUtils
import org.h2.util.HasSQL
import org.h2.util.MathUtils
import org.h2.util.StringUtils
import org.h2.util.Typed
import org.h2.value.TypeInfo.Companion.getTypeInfo
import org.h2.value.ValueBigint.Companion.convertToBigint
import org.h2.value.ValueBinary.Companion.convertToBinary
import org.h2.value.ValueBlob.Companion.convertToBlob
import org.h2.value.ValueClob.Companion.convertToClob
import org.h2.value.ValueDecfloat.Companion.convertToDecfloat
import org.h2.value.ValueVarbinary.Companion.convertToVarbinary
import org.h2.value.lob.LobDataDatabase
import org.h2.value.lob.LobDataInMemory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.Reader
import java.io.StringReader
import java.lang.ref.SoftReference
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.charset.StandardCharsets
import java.util.Arrays
import kotlin.math.roundToLong

/**
 * This is the base class for all value classes.
 * It provides conversion and comparison methods.
 */
abstract class Value : VersionedValue(), HasSQL, Typed {
    companion object {
        /**
         * The data type is unknown at this time.
         */
        const val UNKNOWN = -1

        /**
         * The value type for NULL.
         */
        const val NULL = UNKNOWN + 1

        /**
         * The value type for CHARACTER values.
         */
        const val CHAR = NULL + 1

        /**
         * The value type for CHARACTER VARYING values.
         */
        const val VARCHAR = CHAR + 1

        /**
         * The value type for CHARACTER LARGE OBJECT values.
         */
        const val CLOB = VARCHAR + 1

        /**
         * The value type for VARCHAR_IGNORECASE values.
         */
        const val VARCHAR_IGNORECASE = CLOB + 1

        /**
         * The value type for BINARY values.
         */
        const val BINARY = VARCHAR_IGNORECASE + 1

        /**
         * The value type for BINARY VARYING values.
         */
        const val VARBINARY = BINARY + 1

        /**
         * The value type for BINARY LARGE OBJECT values.
         */
        const val BLOB = VARBINARY + 1

        /**
         * The value type for BOOLEAN values.
         */
        const val BOOLEAN = BLOB + 1

        /**
         * The value type for TINYINT values.
         */
        const val TINYINT = BOOLEAN + 1

        /**
         * The value type for SMALLINT values.
         */
        const val SMALLINT = TINYINT + 1

        /**
         * The value type for INTEGER values.
         */
        const val INTEGER = SMALLINT + 1

        /**
         * The value type for BIGINT values.
         */
        const val BIGINT = INTEGER + 1

        /**
         * The value type for NUMERIC values.
         */
        const val NUMERIC = BIGINT + 1

        /**
         * The value type for REAL values.
         */
        const val REAL = NUMERIC + 1

        /**
         * The value type for DOUBLE PRECISION values.
         */
        const val DOUBLE = REAL + 1

        /**
         * The value type for DECFLOAT values.
         */
        const val DECFLOAT = DOUBLE + 1

        /**
         * The value type for DATE values.
         */
        const val DATE = DECFLOAT + 1

        /**
         * The value type for TIME values.
         */
        const val TIME = DATE + 1

        /**
         * The value type for TIME WITH TIME ZONE values.
         */
        const val TIME_TZ = TIME + 1

        /**
         * The value type for TIMESTAMP values.
         */
        const val TIMESTAMP = TIME_TZ + 1

        /**
         * The value type for TIMESTAMP WITH TIME ZONE values.
         */
        const val TIMESTAMP_TZ = TIMESTAMP + 1

        /**
         * The value type for `INTERVAL YEAR` values.
         */
        const val INTERVAL_YEAR = TIMESTAMP_TZ + 1

        /**
         * The value type for `INTERVAL MONTH` values.
         */
        const val INTERVAL_MONTH = INTERVAL_YEAR + 1

        /**
         * The value type for `INTERVAL DAY` values.
         */
        const val INTERVAL_DAY = INTERVAL_MONTH + 1

        /**
         * The value type for `INTERVAL HOUR` values.
         */
        const val INTERVAL_HOUR = INTERVAL_DAY + 1

        /**
         * The value type for `INTERVAL MINUTE` values.
         */
        const val INTERVAL_MINUTE = INTERVAL_HOUR + 1

        /**
         * The value type for `INTERVAL SECOND` values.
         */
        const val INTERVAL_SECOND = INTERVAL_MINUTE + 1

        /**
         * The value type for `INTERVAL YEAR TO MONTH` values.
         */
        const val INTERVAL_YEAR_TO_MONTH = INTERVAL_SECOND + 1

        /**
         * The value type for `INTERVAL DAY TO HOUR` values.
         */
        const val INTERVAL_DAY_TO_HOUR = INTERVAL_YEAR_TO_MONTH + 1

        /**
         * The value type for `INTERVAL DAY TO MINUTE` values.
         */
        const val INTERVAL_DAY_TO_MINUTE = INTERVAL_DAY_TO_HOUR + 1

        /**
         * The value type for `INTERVAL DAY TO SECOND` values.
         */
        const val INTERVAL_DAY_TO_SECOND = INTERVAL_DAY_TO_MINUTE + 1

        /**
         * The value type for `INTERVAL HOUR TO MINUTE` values.
         */
        const val INTERVAL_HOUR_TO_MINUTE = INTERVAL_DAY_TO_SECOND + 1

        /**
         * The value type for `INTERVAL HOUR TO SECOND` values.
         */
        const val INTERVAL_HOUR_TO_SECOND = INTERVAL_HOUR_TO_MINUTE + 1

        /**
         * The value type for `INTERVAL MINUTE TO SECOND` values.
         */
        const val INTERVAL_MINUTE_TO_SECOND = INTERVAL_HOUR_TO_SECOND + 1

        /**
         * The value type for JAVA_OBJECT values.
         */
        const val JAVA_OBJECT = INTERVAL_MINUTE_TO_SECOND + 1

        /**
         * The value type for ENUM values.
         */
        const val ENUM = JAVA_OBJECT + 1

        /**
         * The value type for string values with a fixed size.
         */
        const val GEOMETRY = ENUM + 1

        /**
         * The value type for JSON values.
         */
        const val JSON = GEOMETRY + 1

        /**
         * The value type for UUID values.
         */
        const val UUID = JSON + 1

        /**
         * The value type for ARRAY values.
         */
        const val ARRAY = UUID + 1

        /**
         * The value type for ROW values.
         */
        const val ROW = ARRAY + 1

        /**
         * The number of value types.
         */
        const val TYPE_COUNT = ROW + 1

        /**
         * Group for untyped NULL data type.
         */
        const val GROUP_NULL = 0

        /**
         * Group for character string data types.
         */
        const val GROUP_CHARACTER_STRING = GROUP_NULL + 1

        /**
         * Group for binary string data types.
         */
        const val GROUP_BINARY_STRING = GROUP_CHARACTER_STRING + 1

        /**
         * Group for BINARY data type.
         */
        const val GROUP_BOOLEAN = GROUP_BINARY_STRING + 1

        /**
         * Group for numeric data types.
         */
        const val GROUP_NUMERIC = GROUP_BOOLEAN + 1

        /**
         * Group for datetime data types.
         */
        const val GROUP_DATETIME = GROUP_NUMERIC + 1

        /**
         * Group for year-month interval data types.
         */
        const val GROUP_INTERVAL_YM = GROUP_DATETIME + 1

        /**
         * Group for day-time interval data types.
         */
        const val GROUP_INTERVAL_DT = GROUP_INTERVAL_YM + 1

        /**
         * Group for other data types (JAVA_OBJECT, UUID, GEOMETRY, ENUM, JSON).
         */
        const val GROUP_OTHER = GROUP_INTERVAL_DT + 1

        /**
         * Group for collection data types (ARRAY, ROW).
         */
        const val GROUP_COLLECTION = GROUP_OTHER + 1


        val GROUPS = byteArrayOf( // NULL
                GROUP_NULL.toByte(),  // CHAR, VARCHAR, CLOB, VARCHAR_IGNORECASE
                GROUP_CHARACTER_STRING.toByte(), GROUP_CHARACTER_STRING.toByte(), GROUP_CHARACTER_STRING.toByte(), GROUP_CHARACTER_STRING.toByte(),  // BINARY, VARBINARY, BLOB
                GROUP_BINARY_STRING.toByte(), GROUP_BINARY_STRING.toByte(), GROUP_BINARY_STRING.toByte(),  // BOOLEAN
                GROUP_BOOLEAN.toByte(),  // TINYINT, SMALLINT, INTEGER, BIGINT, NUMERIC, REAL, DOUBLE, DECFLOAT
                GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(), GROUP_NUMERIC.toByte(),
                GROUP_NUMERIC.toByte(),  // DATE, TIME, TIME_TZ, TIMESTAMP, TIMESTAMP_TZ
                GROUP_DATETIME.toByte(), GROUP_DATETIME.toByte(), GROUP_DATETIME.toByte(), GROUP_DATETIME.toByte(), GROUP_DATETIME.toByte(),  // INTERVAL_YEAR, INTERVAL_MONTH
                GROUP_INTERVAL_YM.toByte(), GROUP_INTERVAL_YM.toByte(),  // INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND
                GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(),  // INTERVAL_YEAR_TO_MONTH
                GROUP_INTERVAL_YM.toByte(),  // INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE,
                // INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE,
                // INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND
                GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(), GROUP_INTERVAL_DT.toByte(),
                GROUP_INTERVAL_DT.toByte(),  // JAVA_OBJECT, ENUM, GEOMETRY, JSON, UUID
                GROUP_OTHER.toByte(), GROUP_OTHER.toByte(), GROUP_OTHER.toByte(), GROUP_OTHER.toByte(), GROUP_OTHER.toByte(),  // ARRAY, ROW
                GROUP_COLLECTION.toByte(), GROUP_COLLECTION.toByte())

        private val NAMES = arrayOf(
                "NULL",  //
                "CHARACTER", "CHARACTER VARYING", "CHARACTER LARGE OBJECT", "VARCHAR_IGNORECASE",  //
                "BINARY", "BINARY VARYING", "BINARY LARGE OBJECT",  //
                "BOOLEAN",  //
                "TINYINT", "SMALLINT", "INTEGER", "BIGINT",  //
                "NUMERIC", "REAL", "DOUBLE PRECISION", "DECFLOAT",  //
                "DATE", "TIME", "TIME WITH TIME ZONE", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE",  //
                "INTERVAL YEAR", "INTERVAL MONTH",  //
                "INTERVAL DAY", "INTERVAL HOUR", "INTERVAL MINUTE", "INTERVAL SECOND",  //
                "INTERVAL YEAR TO MONTH",  //
                "INTERVAL DAY TO HOUR", "INTERVAL DAY TO MINUTE", "INTERVAL DAY TO SECOND",  //
                "INTERVAL HOUR TO MINUTE", "INTERVAL HOUR TO SECOND", "INTERVAL MINUTE TO SECOND",  //
                "JAVA_OBJECT", "ENUM", "GEOMETRY", "JSON", "UUID",  //
                "ARRAY", "ROW")

        /**
         * Empty array of values.
         */
        val EMPTY_VALUES = arrayOfNulls<Value>(0)

        @JvmStatic
        val softCache: SoftReference<Array<Value?>> by lazy { SoftReference<Array<Value?>>(arrayOfNulls(SysProperties.OBJECT_CACHE_SIZE)) }

        @JvmField
        val MAX_LONG_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)

        @JvmField
        val MIN_LONG_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MIN_VALUE)

        /**
         * Convert a value to the specified type without taking scale and precision
         * into account.
         */
        const val CONVERT_TO = 0

        /**
         * Cast a value to the specified type. The scale is set if applicable. The
         * value is truncated to a required precision.
         */
        const val CAST_TO = 1

        /**
         * Cast a value to the specified type for assignment. The scale is set if
         * applicable. If precision is too large an exception is thrown.
         */
        const val ASSIGN_TO = 2

        /**
         * Returns name of the specified data type.
         *
         * @param valueType
         * the value type
         * @return the name
         */
        fun getTypeName(valueType: Int): String? = NAMES[valueType]

        /**
         * Check the range of the parameters.
         * @param zeroBasedOffset the offset (0 meaning no offset)
         * @param length the length of the target
         * @param datasize the length of the source
         */
        @JvmStatic
        inline fun rangeCheck(zeroBasedOffset: Long, length: Long, dataSize: Long): Unit {
            if (zeroBasedOffset or length < 0 || length > dataSize - zeroBasedOffset) {
                if (zeroBasedOffset < 0 || zeroBasedOffset > dataSize) {
                    throw DbException.getInvalidValueException("offset", zeroBasedOffset + 1)
                }
                throw DbException.getInvalidValueException("length", length)
            }
        }

        /**
         * Get the higher value order type of two value types. If values need to be
         * converted to match the other operands value type, the value with the
         * lower order is converted to the value with the higher order.
         *
         * @param t1 the first value type
         * @param t2 the second value type
         * @return the higher value type of the two
         */
        @JvmStatic
        fun getHigherOrder(pt1: Int, pt2: Int): Int {
            var t1 = pt1
            var t2 = pt2

            if (t1 == t2) {
                if (t1 == UNKNOWN)
                    throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?, ?")
                return t1
            }

            if (t1 < t2) t1 = t2.also { t2 = t1 }

            if (t1 == UNKNOWN) {
                if (t2 == NULL) {
                    throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "?, NULL")
                }
                return t2
            } else if (t2 == UNKNOWN) {
                if (t1 == NULL) {
                    throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "NULL, ?")
                }
                return t1
            }
            if (t2 == NULL) return t1
            return getHigherOrderKnown(t1, t2)
        }

        private fun getDataTypeCombinationException(t1: Int, t2: Int): DbException {
            return DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, getTypeName(t1) + ", " + getTypeName(t2))
        }

        private fun getHigherNumeric(t1: Int, t2: Int, g2: Int): Int {
            if (g2 == GROUP_NUMERIC) {
                when (t1) {
                    REAL -> when (t2) {
                        INTEGER -> return DOUBLE
                        BIGINT, NUMERIC -> return DECFLOAT
                    }
                    DOUBLE -> when (t2) {
                        BIGINT, NUMERIC -> return DECFLOAT
                    }
                }
            } else if (g2 == GROUP_BINARY_STRING) {
                throw getDataTypeCombinationException(t1, t2)
            }
            return t1
        }

        private fun getHigherDateTime(t1: Int, t2: Int, g2: Int): Int {
            if (g2 == GROUP_CHARACTER_STRING) return t1
            if (g2 != GROUP_DATETIME) throw getDataTypeCombinationException(t1, t2)
            when (t1) {
                TIME -> if (t2 == DATE) return TIMESTAMP
                TIME_TZ -> if (t2 == DATE) return TIMESTAMP_TZ
                TIMESTAMP -> if (t2 == TIME_TZ) return TIMESTAMP_TZ
            }
            return t1
        }

        private fun getHigherIntervalYearMonth(t1: Int, t2: Int, g2: Int): Int {
            return when (g2) {
                GROUP_INTERVAL_YM -> {
                    if (t1 == INTERVAL_MONTH && t2 == INTERVAL_YEAR) {
                        INTERVAL_YEAR_TO_MONTH
                    } else t1
                }
                GROUP_CHARACTER_STRING, GROUP_NUMERIC -> t1
                else -> throw getDataTypeCombinationException(t1, t2)
            }
        }

        private fun getHigherIntervalDayTime(t1: Int, t2: Int, g2: Int): Int {
            when (g2) {
                GROUP_INTERVAL_DT -> {}
                GROUP_CHARACTER_STRING, GROUP_NUMERIC -> return t1
                else -> throw getDataTypeCombinationException(t1, t2)
            }

            when (t1) {
                INTERVAL_HOUR -> return INTERVAL_DAY_TO_HOUR
                INTERVAL_MINUTE -> {
                    return if (t2 == INTERVAL_DAY) INTERVAL_DAY_TO_MINUTE else INTERVAL_HOUR_TO_MINUTE
                }
                INTERVAL_SECOND -> return when (t2) {
                    INTERVAL_DAY -> INTERVAL_DAY_TO_SECOND
                    INTERVAL_HOUR -> INTERVAL_HOUR_TO_SECOND
                    else -> INTERVAL_MINUTE_TO_SECOND
                }
                INTERVAL_DAY_TO_HOUR -> {
                    when (t2) {
                        INTERVAL_MINUTE -> return INTERVAL_DAY_TO_MINUTE
                        INTERVAL_SECOND -> return INTERVAL_DAY_TO_SECOND
                    }
                }
                INTERVAL_DAY_TO_MINUTE -> if (t2 == INTERVAL_SECOND) return INTERVAL_DAY_TO_SECOND
                INTERVAL_HOUR_TO_MINUTE -> when (t2) {
                    INTERVAL_DAY, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE -> return INTERVAL_DAY_TO_MINUTE
                    INTERVAL_SECOND -> return INTERVAL_HOUR_TO_SECOND
                    INTERVAL_DAY_TO_SECOND -> return INTERVAL_DAY_TO_SECOND
                }
                INTERVAL_HOUR_TO_SECOND -> when (t2) {
                    INTERVAL_DAY, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND -> return INTERVAL_DAY_TO_SECOND
                }
                INTERVAL_MINUTE_TO_SECOND -> when (t2) {
                    INTERVAL_DAY, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND -> return INTERVAL_DAY_TO_SECOND
                    INTERVAL_HOUR, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND -> return INTERVAL_HOUR_TO_SECOND
                }
            }
            return t1
        }

        private fun getHigherOther(t1: Int, t2: Int, g2: Int): Int {
            when (t1) {
                JAVA_OBJECT -> if (g2 != GROUP_BINARY_STRING) throw getDataTypeCombinationException(t1, t2)
                ENUM -> if (g2 != GROUP_CHARACTER_STRING && (g2 != GROUP_NUMERIC || t2 > INTEGER))
                    throw getDataTypeCombinationException(t1, t2)
                GEOMETRY -> if (g2 != GROUP_CHARACTER_STRING && g2 != GROUP_BINARY_STRING)
                    throw getDataTypeCombinationException(t1, t2)
                JSON -> when (g2) {
                    GROUP_DATETIME, GROUP_INTERVAL_YM, GROUP_INTERVAL_DT, GROUP_OTHER -> throw getDataTypeCombinationException(t1, t2)
                }
                UUID -> when (g2) {
                    GROUP_CHARACTER_STRING, GROUP_BINARY_STRING -> {}
                    GROUP_OTHER -> {
                        if (t2 != JAVA_OBJECT) throw getDataTypeCombinationException(t1, t2)
                    }
                    else -> throw getDataTypeCombinationException(t1, t2)
                }
            }
            return t1
        }

        fun getHigherOrderKnown(t1: Int, t2: Int): Int {
            val g1 = GROUPS[t1].toInt()
            val g2 = GROUPS[t2].toInt()
            when (g1) {
                GROUP_BOOLEAN -> if (g2 == GROUP_BINARY_STRING) throw getDataTypeCombinationException(BOOLEAN, t2)
                GROUP_NUMERIC -> return getHigherNumeric(t1, t2, g2)
                GROUP_DATETIME -> return getHigherDateTime(t1, t2, g2)
                GROUP_INTERVAL_YM -> return getHigherIntervalYearMonth(t1, t2, g2)
                GROUP_INTERVAL_DT -> return getHigherIntervalDayTime(t1, t2, g2)
                GROUP_OTHER -> return getHigherOther(t1, t2, g2)
            }
            return t1
        }

        /**
         * Check if a value is in the cache that is equal to this value. If yes,
         * this value should be used to save memory. If the value is not in the
         * cache yet, it is added.
         *
         * @param v the value to look for
         * @return the value in the cache or the value passed
         */
        @JvmStatic
        fun cache(v: Value): Value {
            if (!SysProperties.OBJECT_CACHE) return v
            val cache: Array<Value?> = softCache.get()!!

            val index: Int = v.hashCode() and SysProperties.OBJECT_CACHE_SIZE - 1
            val cached = cache[index]
            if (cached != null
                    && cached.getValueType() == v.getValueType()
                    && v == cached)
                return cached

            cache[index] = v
            return v
        }

        private fun getColumnName(column: Any?): String = column?.toString() ?: ""

        internal fun convertToLong(x: Double, column: Any?): Long {
            if (x > Long.MAX_VALUE || x < Long.MIN_VALUE) {
                // TODO document that +Infinity, -Infinity throw an exception and
                // NaN returns 0
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_2, x.toString(), getColumnName(column))
            }
            return x.roundToLong()
        }

        internal fun convertToLong(x: BigDecimal, column: Any?): Long {
            if (x > MAX_LONG_DECIMAL || x < MIN_LONG_DECIMAL) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_2, x.toString(), getColumnName(column))
            }
            return x.setScale(0, RoundingMode.HALF_UP).toLong()
        }

        private fun convertToByte(x: Long, column: Any?): Byte {
            if (x > Byte.MAX_VALUE || x < Byte.MIN_VALUE) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_2, x.toString(), getColumnName(column))
            }
            return x.toByte()
        }

        private fun convertToShort(x: Long, column: Any): Short {
            if (x > Short.MAX_VALUE || x < Short.MIN_VALUE) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_2, x.toString(), getColumnName(column))
            }
            return x.toShort()
        }

        /**
         * Convert to integer, throwing exception if out of range.
         *
         * @param x integer value.
         * @param column Column info.
         * @return x
         */
        fun convertToInt(x: Long, column: Any?): Int {
            if (x > Int.MAX_VALUE || x < Int.MIN_VALUE) {
                throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_2, x.toString(), getColumnName(column))
            }
            return x.toInt()
        }


    }

    /**
     * Clear the value cache. Used for testing.
     */
    open fun clearCache() = softCache.clear()

    /**
     * Get the SQL expression for this value.
     * @return the SQL expression
     */
    fun getSQL(): String = getSQL(StringBuilder()).toString()

    /**
     * Appends the SQL Expression for this value to the specified builder.
     * @param builder string builder
     * @return the specified string builder
     */
    fun getSQL(builder: StringBuilder): StringBuilder {
        TODO("Not yet implemented")
    }

    /**
     * Get the value type.
     * @return the value type
     */
    abstract fun getValueType(): Int

    /**
     * Java 11 with -XX:-UseCompressedOops for all values up the ValueLong and ValueDouble
     * Get the memeory used by this object.
     * @return the memory used in bytes
     */
    open fun getMemory(): Int = 24

    /**
     * Get the value as a string.
     * @return the string
     */
    abstract fun getString(): String?

    open fun getReader(): Reader? = StringReader(getString())

    /**
     * Get the reader
     *
     * @param oneBasedOffset the offset (1 means no offset)
     * @param length the requested length
     * @return the new reader
     */
    open fun getReader(oneBasedOffset: Long, length: Long): Reader? {
        val string = getString()
        val zeroBasedOffset = oneBasedOffset - 1
        rangeCheck(zeroBasedOffset, length, string!!.length.toLong())
        val offset = zeroBasedOffset.toInt()
        return StringReader(string!!.substring(offset, offset + length.toInt()))
    }


    /**
     * Creates new instance of the DbException for data conversion error.
     *
     * @param targetType Target data type.
     * @return instance of the DbException.
     */
    fun getDataConversionError(targetType: Int): DbException {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, "${getTypeName(getValueType())} to ${getTypeName(targetType)}")
    }

    @Throws(DbException::class)
    open fun getBytes(): ByteArray = throw getDataConversionError(VARBINARY)

    open fun getBytesNoCopy(): ByteArray = getBytes()

    open fun getInputStream(): InputStream? = ByteArrayInputStream(getBytesNoCopy())

    /**
     * Get the input stream
     *
     * @param oneBasedOffset the offset (1 means no offset)
     * @param length the requested length
     * @return the new input stream
     */
    open fun getInputStream(oneBasedOffset: Long, length: Long): InputStream? {
        val bytes = getBytesNoCopy()
        val zeroBasedOffset = oneBasedOffset - 1
        rangeCheck(zeroBasedOffset, length, bytes!!.size.toLong())
        return ByteArrayInputStream(bytes, zeroBasedOffset.toInt(), length.toInt())
    }

    /**
     * Converts this value to a BOOLEAN value. May not be called on a NULL
     * value.
     *
     * @return the BOOLEAN value
     */
    fun convertToBoolean(): ValueBoolean = when (getValueType()) {
        BOOLEAN -> this as ValueBoolean
        CHAR, VARCHAR, VARCHAR_IGNORECASE -> ValueBoolean.get(getBoolean())
        TINYINT, SMALLINT, INTEGER, BIGINT, NUMERIC, DOUBLE, REAL, DECFLOAT -> ValueBoolean.get(getSignum() != 0)
        NULL -> throw DbException.getInternalError()
        else -> throw getDataConversionError(BOOLEAN)
    }

    /**
     * Returns this value as a Java `boolean` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to `BOOLEAN`
     * @return value
     * @see .isTrue
     * @see .isFalse
     */
    open fun getBoolean(): Boolean = convertToBoolean().getBoolean()

    /**
     * Converts this value to a TINYINT value. May not be called on a NULL
     * value.
     *
     * @param column
     * the column, used for to improve the error message if
     * conversion fails
     * @return the TINYINT value
     */
    fun convertToTinyint(column: Any?): ValueTinyint = when (getValueType()) {
        TINYINT -> this as ValueTinyint
        CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN -> ValueTinyint.get(getByte())
        SMALLINT, ENUM, INTEGER -> ValueTinyint.get(Value.convertToByte(getInt().toLong(), column))
        BIGINT, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> ValueTinyint.get(Value.convertToByte(getLong(), column))
        NUMERIC, DECFLOAT -> ValueTinyint.get(Value.convertToByte(Value.convertToLong(getBigDecimal(), column), column))
        REAL, DOUBLE -> ValueTinyint.get(Value.convertToByte(Value.convertToLong(getDouble(), column), column))
        BINARY, VARBINARY -> {
            val bytes = getBytesNoCopy()!!
            if (bytes.size == 1) ValueTinyint.get(bytes[0])
            throw getDataConversionError(TINYINT)
        }
        NULL -> throw DbException.getInternalError()
        else -> throw getDataConversionError(TINYINT)
    }

    /**
     * Converts this value to a INT value. May not be called on a NULL value.
     *
     * @param column
     * the column, used for to improve the error message if
     * conversion fails
     * @return the INT value
     */
    fun convertToInt(column: Any?): ValueInteger = when (getValueType()) {
        INTEGER -> this as ValueInteger
        CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN, TINYINT, ENUM, SMALLINT -> ValueInteger.get(getInt())
        BIGINT, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> ValueInteger.get(Value.convertToInt(getLong(), column))
        NUMERIC, DECFLOAT -> ValueInteger.get(Value.convertToInt(convertToLong(getBigDecimal(), column!!), column))
        REAL, DOUBLE -> ValueInteger.get(Value.convertToInt(convertToLong(getDouble(), column!!), column))
        BINARY, VARBINARY -> {
            val bytes = getBytesNoCopy()
            if (bytes!!.size == 4) ValueInteger.get(Bits.readInt(bytes, 0))
            throw getDataConversionError(INTEGER)
        }
        NULL -> throw DbException.getInternalError()
        else -> throw getDataConversionError(INTEGER)
    }



    /**
     * Returns this value as a Java `byte` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `TINYINT`
     * @return value
     */
    open fun getByte(): Byte = convertToTinyint(null).getByte()


    /**
     * Converts this value to a SMALLINT value. May not be called on a NULL value.
     *
     * @param column
     * the column, used for to improve the error message if
     * conversion fails
     * @return the SMALLINT value
     */
    fun convertToSmallint(column: Any?): ValueSmallint {
        return when (getValueType()) {
            SMALLINT -> this as ValueSmallint
            CHAR, VARCHAR, VARCHAR_IGNORECASE, BOOLEAN, TINYINT -> ValueSmallint.get(getShort())
            ENUM, INTEGER -> ValueSmallint.get(convertToShort(getInt().toLong(), column!!))
            BIGINT, INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_YEAR_TO_MONTH, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> ValueSmallint.get(convertToShort(getLong(), column!!))
            NUMERIC, DECFLOAT -> ValueSmallint.get(convertToShort(convertToLong(getBigDecimal(), column), column!!))
            REAL, DOUBLE -> ValueSmallint.get(convertToShort(convertToLong(getDouble(), column), column!!))
            BINARY, VARBINARY -> {
                val bytes = getBytesNoCopy()!!
                if (bytes.size == 2) {
                    return ValueSmallint.get(((bytes[0].toInt() shl 8) + (bytes[1].toInt() and 0xff)).toShort())
                }
                throw getDataConversionError(SMALLINT)
            }
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(SMALLINT)
        }
    }

    /**
     * Returns this value as a Java `short` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `SMALLINT`
     * @return value
     */
    open fun getShort(): Short = convertToSmallint(null).getShort()

    /**
     * Returns this value as a Java `int` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `INTEGER`
     * @return value
     */
    open fun getInt(): Int = convertToInt(null).int

    /**
     * Returns this value as a Java `long` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `BIGINT`
     * @return value
     */
    open fun getLong(): Long = convertToBigint(null).getLong()

    open fun getBigDecimal(): BigDecimal = throw getDataConversionError(NUMERIC)

    /**
     * Returns this value as a Java `float` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `REAL`
     * @return value
     */
    open fun getFloat(): Float = throw getDataConversionError(REAL)

    /**
     * Returns this value as a Java `double` value.
     *
     * @throws DbException
     * if this value is `NULL` or cannot be casted to
     * `DOUBLE PRECISION`
     * @return value
     */
    open fun getDouble(): Double = throw getDataConversionError(DOUBLE)

    abstract override fun hashCode(): Int

    /**
     * Check if the two values have the same hash code. No data conversion is made;
     * this method returns false if the other object is not of the same class.
     * For some values, compareTo may return 0 even if equals return false.
     * Example: ValueDecimal 0.0 and 0.00
     * @param other the other value
     * @return true if they are equal
     */
    abstract override fun equals(other: Any?): Boolean

    /**
     * Add a value and return the result.
     *
     * @param v the value to add
     * @return the result
     */
    @Throws(DbException::class)
    open fun add(v: Value?): Value = throw getUnsupportedExceptionForOperation("+")

    open fun getSignum(): Int = throw getUnsupportedExceptionForOperation("SIGNUM")

    /**
     * Return -value if this value support arithmetic operations.
     *
     * @return the negative
     */
    @Throws(DbException::class)
    open fun negate(): Value = throw getUnsupportedExceptionForOperation("NEG")

    /**
     * Subtract a value and return the result.
     *
     * @param v the value to subtract
     * @return the result
     */
    @Throws(DbException::class)
    open fun subtract(v: Value?): Value = throw getUnsupportedExceptionForOperation("-")

    /**
     * Divide by a value and return the result.
     *
     * @param v the divisor
     * @param quotientType the type of quotient (used only to read precision and scale
     * when applicable)
     * @return the result
     */
    @Throws(DbException::class)
    open fun divide(v: Value?, quotientType: TypeInfo?): Value = throw getUnsupportedExceptionForOperation("/")

    /**
     * Multiply with a value and return the result.
     *
     * @param v the value to multiply with
     * @return the result
     */
    @Throws(DbException::class)
    open fun multiply(v: Value?): Value = throw getUnsupportedExceptionForOperation("*")

    /**
     * Take the modulus with a value and return the result.
     *
     * @param v the value to take the modulus with
     * @return the result
     */
    @Throws(DbException::class)
    open fun modulus(v: Value?): Value = throw getUnsupportedExceptionForOperation("%")


    fun getValueTooLongException(targetType: TypeInfo, column: Any?): DbException {
        val builder = StringBuilder()
        if (column != null) {
            builder.append(column).append(' ')
        }
        targetType.getSQL(builder, HasSQL.TRACE_SQL_FLAGS)
        return DbException.getValueTooLongException(builder.toString(), getTraceSQL()!!, type?.precision!!)
    }

    /**
     * Convert a value to the specified type.
     *
     * @param targetType the type of the returned value
     * @param provider the cast information provider
     * @param conversionMode conversion mode
     * @param column the column (if any), used to improve the error message if conversion fails
     * @return the converted value
     */
    fun convertTo(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int, column: Any): Value {
        val valueType = getValueType()
        val targetValueType: Int = targetType.valueType

        return if (valueType == NULL
                || valueType == targetValueType//.also { targetValueType = it }
                && conversionMode == CONVERT_TO
                && targetType.valueType == null
                && valueType != CHAR) {
            this
        } else when (targetValueType) {
            NULL -> ValueNull.INSTANCE
            CHAR -> convertToChar(targetType, provider, conversionMode, column)
            VARCHAR -> convertToVarchar(targetType, provider, conversionMode, column)
            CLOB -> convertToClob(targetType, conversionMode, column)
            VARCHAR_IGNORECASE -> convertToVarcharIgnoreCase(targetType, conversionMode, column)
            BINARY -> convertToBinary(targetType, conversionMode, column)
            VARBINARY -> convertToVarbinary(targetType, conversionMode, column)
            BLOB -> convertToBlob(targetType, conversionMode, column)
            BOOLEAN -> convertToBoolean()
            TINYINT -> convertToTinyint(column)
            SMALLINT -> convertToSmallint(column)
            INTEGER -> convertToInt(column)
            BIGINT -> convertToBigint(column)
            NUMERIC -> convertToNumeric(targetType, provider, conversionMode, column)
            REAL -> convertToReal()
            DOUBLE -> convertToDouble()
            DECFLOAT -> convertToDecfloat(targetType, conversionMode)
            DATE -> convertToDate(provider)
            TIME -> convertToTime(targetType, provider, conversionMode)
            TIME_TZ -> convertToTimeTimeZone(targetType, provider, conversionMode)
            TIMESTAMP -> convertToTimestamp(targetType, provider, conversionMode)
            TIMESTAMP_TZ -> convertToTimestampTimeZone(targetType, provider, conversionMode)
            INTERVAL_YEAR, INTERVAL_MONTH, INTERVAL_YEAR_TO_MONTH -> convertToIntervalYearMonth(targetType, conversionMode, column)
            INTERVAL_DAY, INTERVAL_HOUR, INTERVAL_MINUTE, INTERVAL_SECOND, INTERVAL_DAY_TO_HOUR, INTERVAL_DAY_TO_MINUTE, INTERVAL_DAY_TO_SECOND, INTERVAL_HOUR_TO_MINUTE, INTERVAL_HOUR_TO_SECOND, INTERVAL_MINUTE_TO_SECOND -> convertToIntervalDayTime(targetType, conversionMode, column)
            JAVA_OBJECT -> convertToJavaObject(targetType, conversionMode, column)
            ENUM -> convertToEnum(targetType.getExtTypeInfo() as ExtTypeInfoEnum?, provider)
            GEOMETRY -> convertToGeometry(targetType.getExtTypeInfo() as ExtTypeInfoGeometry?)
            JSON -> convertToJson(targetType, conversionMode, column)
            UUID -> convertToUuid()
            ARRAY -> convertToArray(targetType, provider, conversionMode, column)
            ROW -> convertToRow(targetType, provider, conversionMode, column)
            else -> throw getDataConversionError(targetValueType)
        }
    }

    open fun convertToChar(targetType: TypeInfo, provider: CastDataProvider?, conversionMode: Int, column: Any?): ValueChar {
        val valueType: Int = getValueType()
        when (valueType) {
            BLOB, JAVA_OBJECT -> throw getDataConversionError(targetType.valueType)
        }

        var s = getString()!!
        val length = s.length
        var newLength = length

        if (conversionMode == CONVERT_TO) {
            while (newLength > 0 && s[newLength - 1] == ' ') newLength--
        } else {
            val thePrecision = MathUtils.convertLongToInt(targetType.precision)

            if (provider == null || provider.mode.charPadding == CharPadding.ALWAYS) {
                if (newLength != thePrecision) {
                    if (newLength < thePrecision) {
                        return ValueChar.get(StringUtils.pad(s, thePrecision, null, true))
                    } else if (conversionMode == CAST_TO) {
                        newLength = thePrecision
                    } else {
                        do {
                            if (s[--newLength] != ' ') {
                                throw getValueTooLongException(targetType, column)
                            }
                        } while (newLength > thePrecision)
                    }
                }
            } else {
                if (conversionMode == CAST_TO && newLength > thePrecision) {
                    newLength = thePrecision
                }

                while (newLength > 0 && s[newLength - 1] == ' ') newLength--
                if (conversionMode == ASSIGN_TO && newLength > thePrecision) throw getValueTooLongException(targetType, column)
            }
        }
        if (length != newLength) {
            s = s.substring(0, newLength)
        } else if (valueType == CHAR) {
            return this as ValueChar
        }
        return ValueChar.get(s)
    }

    private fun convertToVarchar(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int, column: Any): Value {
        val valueType = getValueType()
        when (valueType) {
            BLOB, JAVA_OBJECT -> throw getDataConversionError(targetType.valueType)
        }

        if (conversionMode != CONVERT_TO) {
            val s = getString()
            val p = MathUtils.convertLongToInt(targetType.precision)
            if (s!!.length > p) {
                if (conversionMode != CAST_TO) throw getValueTooLongException(targetType, column)
                return ValueVarchar.get(s.substring(0, p), provider)
            }
        }
        return if (valueType == VARCHAR) this else ValueVarchar.get(getString(), provider)
    }

    private fun convertToVarcharIgnoreCase(targetType: TypeInfo, conversionMode: Int, column: Any): Value {
        val valueType = getValueType()
        when (valueType) {
            BLOB, JAVA_OBJECT -> throw getDataConversionError(targetType.valueType)
        }

        if (conversionMode == CONVERT_TO)
            return if (valueType == VARCHAR_IGNORECASE) this else ValueVarcharIgnoreCase.get(getString())

        val s = getString()
        val p = MathUtils.convertLongToInt(targetType.precision)

        if (s!!.length > p) {
            if (conversionMode == CAST_TO) {
                return ValueVarcharIgnoreCase.get(s.substring(0, p))
            }
            throw getValueTooLongException(targetType, column)
        }

        return if (valueType == VARCHAR_IGNORECASE) this else ValueVarcharIgnoreCase.get(getString())
    }

    private fun convertToNumeric(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int, column: Any): ValueNumeric {
        var v: ValueNumeric
        when (getValueType()) {
            NUMERIC -> v = this as ValueNumeric
            BOOLEAN -> v = if (getBoolean()) ValueNumeric.ONE else ValueNumeric.ZERO
            NULL -> throw DbException.getInternalError()
            else -> {
                var value = getBigDecimal()
                val targetScale: Int = targetType.scale
                val scale = value.scale()
                if (scale < 0 || scale > ValueNumeric.MAXIMUM_SCALE
                        || conversionMode != CONVERT_TO
                        && scale != targetScale
                        && (scale >= targetScale || !provider.mode.convertOnlyToSmallerScale)) {
                    value = ValueNumeric.setScale(value, targetScale)
                }
                if (conversionMode != CONVERT_TO && value.precision() > targetType.precision - targetScale + value.scale()) {
                    throw getValueTooLongException(targetType, column)
                }
                return ValueNumeric.get(value)
            }
        }

        if (conversionMode == CONVERT_TO) return v

        val targetScale: Int = targetType.scale
        val value = v.bigDecimal
        val scale = value.scale()
        if (scale != targetScale && (scale >= targetScale || !provider.mode.convertOnlyToSmallerScale)) {
            v = ValueNumeric.get(ValueNumeric.setScale(value, targetScale))
        }
        val bd = v.bigDecimal
        if (bd.precision() > targetType.precision - targetScale + bd.scale()) {
            throw v.getValueTooLongException(targetType, column)
        }
        return v
    }

    /**
     * Converts this value to a REAL value. May not be called on a NULL value.
     *
     * @return the REAL value
     */
    fun convertToReal(): ValueReal = when (getValueType()) {
        REAL -> this as ValueReal
        BOOLEAN -> if (getBoolean()) ValueReal.ONE else ValueReal.ZERO
        NULL -> throw DbException.getInternalError()
        else -> ValueReal.get(getFloat())
    }

    /**
     * Converts this value to a DOUBLE value. May not be called on a NULL value.
     *
     * @return the DOUBLE value
     */
    fun convertToDouble(): ValueDouble = when (getValueType()) {
        DOUBLE -> this as ValueDouble
        BOOLEAN -> if (getBoolean()) ValueDouble.ONE else ValueDouble.ZERO
        NULL -> throw DbException.getInternalError()
        else -> ValueDouble.get(getDouble())
    }

    /**
     * Converts this value to a DATE value. May not be called on a NULL value.
     *
     * @param provider
     * the cast information provider
     * @return the DATE value
     */
    fun convertToDate(provider: CastDataProvider): ValueDate = when (getValueType()) {
        DATE -> this as ValueDate
        TIMESTAMP -> ValueDate.fromDateValue((this as ValueTimestamp).dateValue)
        TIMESTAMP_TZ -> {
            val ts = this as ValueTimestampTimeZone
            val timeNanos = ts.timeNanos
            val epochSeconds: Long = DateTimeUtils.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)
            ValueDate.fromDateValue(DateTimeUtils.dateValueFromLocalSeconds(epochSeconds + provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds)))
        }
        VARCHAR, VARCHAR_IGNORECASE, CHAR -> ValueDate.parse(getString()!!.trim { it <= ' ' })
        NULL -> throw DbException.getInternalError()
        else -> throw getDataConversionError(DATE)
    }

    private fun getLocalTimeNanos(provider: CastDataProvider): Long {
        val ts = this as ValueTimeTimeZone
        val localOffset = provider.currentTimestamp().timeZoneOffsetSeconds
        return DateTimeUtils.normalizeNanosOfDay(ts.nanos + (ts.timeZoneOffsetSeconds - localOffset) * DateTimeUtils.NANOS_PER_DAY)
    }

    private fun convertToTime(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTime {
        var v: ValueTime = when (getValueType()) {
            TIME -> this as ValueTime
            TIME_TZ -> ValueTime.fromNanos(getLocalTimeNanos(provider))
            TIMESTAMP -> ValueTime.fromNanos((this as ValueTimestamp).timeNanos)
            TIMESTAMP_TZ -> {
                val ts = this as ValueTimestampTimeZone
                val timeNanos = ts.timeNanos
                val epochSeconds = DateTimeUtils.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)

                ValueTime.fromNanos(DateTimeUtils.nanosFromLocalSeconds(epochSeconds
                        + provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds))
                        + timeNanos % DateTimeUtils.NANOS_PER_SECOND)
            }
            VARCHAR, VARCHAR_IGNORECASE, CHAR -> ValueTime.parse(getString()!!.trim { it <= ' ' })
            else -> throw getDataConversionError(TIME)
        }

        if (conversionMode == CONVERT_TO) return v

        val targetScale: Int = targetType.scale
        if (targetScale < ValueTime.MAXIMUM_SCALE) {
            val n = v.nanos
            val n2: Long = DateTimeUtils.convertScale(n, targetScale, DateTimeUtils.NANOS_PER_DAY)
            if (n2 != n) {
                v = ValueTime.fromNanos(n2)
            }
        }
        return v
    }

    private fun convertToTimeTimeZone(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTimeTimeZone {
        var v: ValueTimeTimeZone = when (getValueType()) {
            TIME_TZ -> this as ValueTimeTimeZone
            TIME -> ValueTimeTimeZone.fromNanos((this as ValueTime).nanos,
                    provider.currentTimestamp().timeZoneOffsetSeconds)
            TIMESTAMP -> {
                val ts = this as ValueTimestamp
                val timeNanos = ts.timeNanos
                ValueTimeTimeZone.fromNanos(timeNanos, provider.currentTimeZone().getTimeZoneOffsetLocal(ts.dateValue, timeNanos))
            }
            TIMESTAMP_TZ -> {
                val ts = this as ValueTimestampTimeZone
                ValueTimeTimeZone.fromNanos(ts.timeNanos, ts.timeZoneOffsetSeconds)
            }
            VARCHAR, VARCHAR_IGNORECASE, CHAR -> ValueTimeTimeZone.parse(getString()!!.trim { it <= ' ' })
            else -> throw getDataConversionError(TIME_TZ)
        }

        if (conversionMode == CONVERT_TO) return v

        val targetScale: Int = targetType.scale
        if (targetScale < ValueTime.MAXIMUM_SCALE) {
            val n = v.nanos
            val n2 = DateTimeUtils.convertScale(n, targetScale, DateTimeUtils.NANOS_PER_DAY)
            if (n2 != n) {
                v = ValueTimeTimeZone.fromNanos(n2, v.timeZoneOffsetSeconds)
            }
        }
        return v
    }

    private fun convertToTimestamp(targetType: TypeInfo, provider: CastDataProvider, conversionMode: Int): ValueTimestamp? {
        var v: ValueTimestamp
        when (getValueType()) {
            TIMESTAMP -> v = this as ValueTimestamp
            TIME -> v = ValueTimestamp.fromDateValueAndNanos(provider.currentTimestamp().dateValue, (this as ValueTime).nanos)
            TIME_TZ -> v = ValueTimestamp.fromDateValueAndNanos(provider.currentTimestamp().dateValue, getLocalTimeNanos(provider))
            DATE ->             // Scale is always 0
                return ValueTimestamp.fromDateValueAndNanos((this as ValueDate).dateValue, 0)
            TIMESTAMP_TZ -> {
                val ts = this as ValueTimestampTimeZone
                val timeNanos = ts.timeNanos
                var epochSeconds = DateTimeUtils.getEpochSeconds(ts.dateValue, timeNanos, ts.timeZoneOffsetSeconds)
                epochSeconds += provider.currentTimeZone().getTimeZoneOffsetUTC(epochSeconds).toLong()
                v = ValueTimestamp.fromDateValueAndNanos(DateTimeUtils.dateValueFromLocalSeconds(epochSeconds),
                        DateTimeUtils.nanosFromLocalSeconds(epochSeconds) + timeNanos % DateTimeUtils.NANOS_PER_SECOND)
            }
            VARCHAR, VARCHAR_IGNORECASE, CHAR -> v = ValueTimestamp.parse(getString()!!.trim { it <= ' ' }, provider)
            else -> throw getDataConversionError(TIMESTAMP)
        }

        if (conversionMode != CONVERT_TO) {
            val targetScale: Int = targetType.scale
            if (targetScale < ValueTimestamp.MAXIMUM_SCALE) {
                var dv = v.dateValue
                val n = v.timeNanos
                var n2 = DateTimeUtils.convertScale(n, targetScale,
                        if (dv == DateTimeUtils.MAX_DATE_VALUE) DateTimeUtils.NANOS_PER_DAY else Long.MAX_VALUE)
                if (n2 != n) {
                    if (n2 >= DateTimeUtils.NANOS_PER_DAY) {
                        n2 -= DateTimeUtils.NANOS_PER_DAY
                        dv = DateTimeUtils.incrementDateValue(dv)
                    }
                    v = ValueTimestamp.fromDateValueAndNanos(dv, n2)
                }
            }
        }
        return v
    }

    /**
     * Convert a value to the specified type without taking scale and precision
     * into account.
     *
     * @param targetType the type of the returned value
     * @param provider the cast information provider
     * @return the converted value
     */
    fun convertTo(targetType: Int, provider: CastDataProvider?): Value {
        return when (targetType) {
            ARRAY -> convertToAnyArray(provider)
            ROW -> convertToAnyRow()
            else -> convertTo(getTypeInfo(targetType), provider, CONVERT_TO, null)
        }
    }

    /**
     * Convert a value to the specified type without taking scale and precision
     * into account.
     *
     * @param targetType the type of the returned value
     * @return the converted value
     */
    fun convertTo(targetType: Int): Value = convertTo(targetType, null)

    /**
     * Create an exception meaning the specified operation is not supported for
     * this data type.
     *
     * @param op the operation
     * @return the exception
     */
    @Throws(DbException::class)
    protected fun getUnsupportedExceptionForOperation(op: String): DbException =
            DbException.getUnsupportedException("${DataType.getDataType(getValueType()).name.toString()} $op")


    /**
     * Compare this value against another value given that the values are of the
     * same data type.
     *
     * @param v the other value
     * @param mode the compare mode
     * @param provider the cast information provider
     * @return 0 if both values are equal, -1 if the other value is smaller, and
     * 1 otherwise
     */
    abstract fun compareTypeSafe(v: Value?, mode: CompareMode?, provider: CastDataProvider?): Int

    /**
     * Returns length of this value in characters.
     *
     * @return length of this value in characters
     * @throws NullPointerException if this value is `NULL`
     */
    open fun charLength(): Long = getString()!!.length.toLong()

    /**
     * Returns length of this value in bytes.
     *
     * @return length of this value in bytes
     * @throws NullPointerException if this value is `NULL`
     */
    open fun octetLength(): Long =  getBytesNoCopy().size.toLong()
}