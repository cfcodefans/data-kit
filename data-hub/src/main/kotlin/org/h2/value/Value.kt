package org.h2.value

import org.h2.engine.SysProperties
import org.h2.message.DbException
import java.lang.ref.SoftReference
import java.math.BigDecimal

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
    abstract fun getType():TypeInfo
}