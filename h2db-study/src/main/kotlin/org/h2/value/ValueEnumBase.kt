package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.util.StringUtils
import java.math.BigDecimal

/**
 * Base implementation of the ENUM data type.
 *
 * This base implementation is only used in 2.0.* clients when they work with
 * 1.4.* servers.
 */
open class ValueEnumBase(val label: String, val ordinal: Int) : Value() {

    companion object {
        /**
         * Get or create an enum value with the given label and ordinal.
         *
         * @param label the label
         * @param ordinal the ordinal
         * @return the value
         */
        operator fun get(label: String?, ordinal: Int): ValueEnumBase? = ValueEnumBase(label!!, ordinal)

    }

    override fun add(v: Value): Value = convertToInt(null).add(v.convertToInt(null))

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int = getInt().compareTo(v.getInt())

    override fun divide(v: Value, quotientType: TypeInfo?): Value = convertToInt(null).divide(v.convertToInt(null), quotientType)

    override fun equals(other: Any?): Boolean = other is ValueEnumBase && getInt() == other.getInt()

    override fun getInt(): Int = ordinal

    override fun getLong(): Long = ordinal.toLong()

    override fun getBigDecimal(): BigDecimal = BigDecimal.valueOf(ordinal.toLong())

    override fun getFloat(): Float = ordinal.toFloat()

    override fun getDouble(): Double = ordinal.toDouble()

    override fun getSignum(): Int = Integer.signum(ordinal)

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = StringUtils.quoteStringSQL(builder, label)

    override fun getString(): String = label

    override val type: TypeInfo
        get() = TypeInfo.TYPE_ENUM_UNDEFINED

    override fun getValueType(): Int = ENUM

    override fun getMemory(): Int = 120

    override fun hashCode(): Int = 31 + getString().hashCode() + getInt()

    override fun modulus(v: Value): Value = convertToInt(null).modulus(v.convertToInt(null))

    override fun multiply(v: Value): Value = convertToInt(null).multiply(v.convertToInt(null))

    override fun subtract(v: Value): Value = convertToInt(null).subtract(v.convertToInt(null))
}