package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import java.math.BigDecimal

/**
 * Implementation of the BOOLEAN data type.
 */
class ValueBoolean(val value: Boolean) : Value() {
    companion object {
        /**
         * The precision in digits.
         */
        const val PRECISION = 1

        /**
         * The maximum display size of a boolean.
         * Example: FALSE
         */
        const val DISPLAY_SIZE = 5

        /**
         * TRUE value.
         */
        val TRUE = ValueBoolean(true)

        /**
         * FALSE value.
         */
        val FALSE = ValueBoolean(false)

        /**
         * Get the boolean value for the given boolean.
         *
         * @param b the boolean
         * @return the value
         */
        operator fun get(b: Boolean): ValueBoolean = if (b) TRUE else FALSE

        /**
         * Converts this value to a BOOLEAN value. May not be called on a NULL
         * value.
         *
         * @return the BOOLEAN value
         */
        fun Value.convertToBoolean(): ValueBoolean = when (getValueType()) {
            BOOLEAN -> this as ValueBoolean
            CHAR, VARCHAR, VARCHAR_IGNORECASE -> ValueBoolean.get(getBoolean())
            TINYINT, SMALLINT, INTEGER, BIGINT, NUMERIC, DOUBLE, REAL, DECFLOAT -> ValueBoolean.get(getSignum() != 0)
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(BOOLEAN)
        }
    }

    override val type: TypeInfo = TypeInfo.TYPE_BOOLEAN

    override fun getValueType(): Int = BOOLEAN

    override fun getMemory(): Int = 0

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = builder.append(getString())

    override fun getString(): String? = if (value) "TRUE" else "FALSE"

    override fun getBoolean(): Boolean = value

    override fun getByte(): Byte = if (value) 1.toByte() else 0.toByte()

    override fun getShort(): Short = if (value) 1.toShort() else 0.toShort()

    override fun getInt(): Int = if (value) 1 else 0

    override fun getLong(): Long = if (value) 1L else 0L

    override fun getBigDecimal(): BigDecimal = if (value) BigDecimal.ONE else BigDecimal.ZERO

    override fun getFloat(): Float = if (value) 1f else 0f

    override fun getDouble(): Double = if (value) 1.0 else 0.0

    override fun negate(): Value = if (value) FALSE else TRUE

    override fun hashCode(): Int = if (value) 1 else 0

    // there are only ever two instances, so the instance must match
    override fun equals(other: Any?): Boolean = this === other

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int =
            value.compareTo((o as ValueBoolean).value)
}