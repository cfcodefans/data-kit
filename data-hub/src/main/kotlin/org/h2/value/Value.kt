package org.h2.value

import org.h2.engine.SysProperties
import org.h2.message.DbException
import java.lang.ref.SoftReference
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.SQLException

/**
 * This is the base class for all value classes.
 * It provides conversion and comparison methods.
 */
abstract class Value : VersionedValue() {
    companion object {
        /**
         * The data type is unknown at this time.
         */
        const val UNKNOWN: Int = -1

        /**
         * The value type of NULL.
         */
        const val NULL: Int = 0

        /**
         * The value type for BOOLEAN values.
         */
        const val BOOLEAN: Int = 1

        /**
         * The value type for BYTE values.
         */
        const val BYTE: Int = 2

        /**
         * The value type for SHORT values.
         */
        const val SHORT: Int = 3

        /**
         * The value type for INT values.
         */
        const val INT: Int = 4

        /**
         * The value type for LONG values.
         */
        const val LONG: Int = 5

        /**
         * The value type for DECIMAL values.
         */
        const val DECIMAL: Int = 6

        /**
         * The value type for DOUBLE values.
         */
        const val DOUBLE: Int = 7

        /**
         * The value type for FLOAT values.
         */
        const val FLOAT: Int = 8

        /**
         * The value type for TIME values.
         */
        const val TIME: Int = 9

        /**
         * The value type for DATE values.
         */
        const val DATE: Int = 10

        /**
         * The value type for TIMESTAMP values.
         */
        const val TIMESTAMP: Int = 11

        /**
         * The value type for BYTES values.
         */
        const val BYTES: Int = 12

        /**
         * The value type for STRING values.
         */
        const val STRING: Int = 13

        /**
         * The value type for case insensitive STRING values.
         */
        const val STRING_IGNORECASE: Int = 14

        /**
         * The value type for BLOB values.
         */
        const val BLOB: Int = 15

        /**
         * The value type for CLOB values.
         */
        const val CLOB: Int = 16

        /**
         * The value type for ARRAY values.
         */
        const val ARRAY: Int = 17

        /**
         * The value type for RESULT_SET values.
         */
        const val RESULT_SET: Int = 18

        /**
         * The value type for JAVA_OBJECT values.
         */
        const val JAVA_OBJECT: Int = 19

        /**
         * The value type for UUID values.
         */
        const val UUID: Int = 20

        /**
         * The value type for string values with a fixed size.
         */
        const val STRING_FIXED: Int = 21

        const val GEOMETRY: Int = 22

        /**
         * 23 was a short-lived experiment "TIMESTAMP UTC" which has been removed.
         */

        /**
         * The value type for TIMESTAMP WITH TIME ZONE values.
         */
        const val TIMESTAMP_TZ: Int = 24

        /**
         * The value type for ENUM values.
         */
        const val ENUM: Int = 25

        /**
         * The value type for {@code INTERVAL YEAR} values.
         */
        const val INTERVAL_YEAR: Int = 26

        /**
         * The value type for {@code INTERVAL SECOND} values.
         */
        const val INTERVAL_MONTH: Int = 27

        /**
         * The value type for {@code INTERVAL DAY} values.
         */
        const val INTERVAL_DAY: Int = 28

        /**
         * The value type for {@code INTERVAL HOUR} values.
         */
        const val INTERVAL_HOUR: Int = 29

        /**
         * The value type for {@code INTERVAL MINUTE} values.
         */
        const val INTERVAL_MINUTE: Int = 30

        /**
         * The value type for {@code INTERVAL SECOND} values.
         */
        const val INTERVAL_SECOND: Int = 31

        /**
         * The value type for {@code INTERVAL_YEAR_TO_MONTH} values.
         */
        const val INTERVAL_YEAR_TO_MONTH: Int = 32

        /**
         * The value type for {@code INTERVAL_DAY_TO_HOUR} values.
         */
        const val INTERVAL_DAY_TO_HOUR: Int = 33

        /**
         * The value type for {@code INTERVAL_DAY_TO_MINUTE} values.
         */
        const val INTERVAL_DAY_TO_MINUTE: Int = 34

        /**
         * The value type for {@code INTERVAL_DAY_TO_SECOND} values.
         */
        const val INTERVAL_DAY_TO_SECOND: Int = 35

        /**
         * The value type for {@code INTERVAL_HOUR_TO_MINUTE} values.
         */
        const val INTERVAL_HOUR_TO_MINUTE: Int = 36

        /**
         * The value type for {@code INTERVAL_HOUR_TO_SECONDE} values.
         */
        const val INTERVAL_HOUR_TO_SECOND: Int = 37

        /**
         * The value type for {@code INTERVAL MINUTE TO SECOND} values.
         */
        const val INTERVAL_MINUTE_TO_SECOND: Int = 38
        /**
         * The value type for ROW values.
         */
        const val ROW: Int = 39

        /**
         * The value type for JSON values.
         */
        const val JSON: Int = 40
        /**
         * The number of value types.
         */
        const val TYPE_COUNT: Int = JSON + 1

        @JvmStatic
        val softCache: SoftReference<Array<Value?>> by lazy { SoftReference<Array<Value?>>(arrayOfNulls(SysProperties.OBJECT_CACHE_SIZE)) }

        @JvmField
        val MAX_LONG_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MAX_VALUE)
        @JvmField
        val MIN_LONG_DECIMAL: BigDecimal = BigDecimal.valueOf(Long.MIN_VALUE)

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
         * Get the order of this value type.
         * @param type the value type
         * @return the order number
         */
        @JvmStatic
        fun getOrder(type: Int): Int = when (type) {
            UNKNOWN -> 1_000
            NULL -> 2_000
            STRING -> 10_000
            CLOB -> 11_000
            STRING_FIXED -> 12_000
            STRING_IGNORECASE -> 13_000
            BOOLEAN -> 20_000
            BYTE -> 21_000
            SHORT -> 22_000
            INT -> 23_000
            LONG -> 24_000
            DECIMAL -> 25_000
            FLOAT -> 26_000
            DOUBLE -> 27_000
            INTERVAL_YEAR -> 28_000
            INTERVAL_MONTH -> 28_100
            INTERVAL_YEAR_TO_MONTH -> 28_200
            INTERVAL_DAY -> 29_000
            INTERVAL_HOUR -> 29_100
            INTERVAL_DAY_TO_HOUR -> 29_200
            INTERVAL_MINUTE -> 29_300
            INTERVAL_HOUR_TO_MINUTE -> 29_400
            INTERVAL_DAY_TO_MINUTE -> 29_500
            INTERVAL_SECOND -> 29_600
            INTERVAL_MINUTE_TO_SECOND -> 29_700
            INTERVAL_HOUR_TO_SECOND -> 29_800
            INTERVAL_DAY_TO_SECOND -> 29_900
            TIME -> 30_000
            DATE -> 30_100
            TIMESTAMP -> 32_000
            TIMESTAMP_TZ -> 34_000
            BYTES -> 40_000
            BLOB -> 41_000
            JAVA_OBJECT -> 42_000
            UUID -> 43_000
            GEOMETRY -> 44_000
            ENUM -> 45_000
            JSON -> 46_000
            ARRAY -> 50_000
            ROW -> 51_000
            RESULT_SET -> 52_000
            else -> TODO()
        }
    }

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
    abstract fun getSQL(builder: StringBuilder): StringBuilder

    /**
     * Returns the data type.
     * @return the data type
     */
    abstract fun getType(): TypeInfo

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
    fun getMemory(): Int = 24

    /**
     * Get the value as a string.
     * @return the string
     */
    abstract fun getString(): String

    /**
     * Get the value as an object.
     * @return the object
     */
    abstract fun getObject(): Any

    /**
     * Set the value as a parameter in a prepared statement.
     * @param prep the prepared statement
     * @param parameterIndex the parameter index
     */
    @Throws(SQLException::class)
    abstract fun set(prep: PreparedStatement, parameterIndex: Int)

    /**
     * Check if the two values have the same hash code. No data conversion is made;
     * this method returns false if the other object is not of the same class.
     * For some values, compareTo may return 0 even if equals return false.
     * Example: ValueDecimal 0.0 and 0.00
     * @param other the other value
     * @return true if they are equal
     */
    abstract override fun equals(other: Any?): Boolean


}