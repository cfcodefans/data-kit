package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets

/**
 * Base implementation of String based data types.
 */
abstract class ValueStringBase(val value: String) : Value() {
    init {
        if (value.length > Constants.MAX_STRING_LENGTH)
            throw DbException.getValueTooLongException(getTypeName(getValueType()), value, value.length.toLong())
    }

    override val type: TypeInfo? by lazy {
        TypeInfo(getValueType(), this.value.length.toLong(), 0, null)
    }

    override fun hashCode(): Int {
        // TODO hash performance: could build a quicker hash
        // by hashing the size and a few characters
        return javaClass.hashCode() xor value.hashCode()
    }

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return mode!!.compareString(value, (v as ValueStringBase).value, false)
    }

    override fun getString(): String = value

    override fun getBytes(): ByteArray = value.toByteArray(StandardCharsets.UTF_8)

    override fun getBoolean(): Boolean {
        val s = value
        return if (s.equals("true", ignoreCase = true)
                || s.equals("t", ignoreCase = true)
                || s.equals("yes", ignoreCase = true)
                || s.equals("y", ignoreCase = true)) {
            true
        } else if (s.equals("false", ignoreCase = true)
                || s.equals("f", ignoreCase = true)
                || s.equals("no", ignoreCase = true)
                || s.equals("n", ignoreCase = true)) {
            false
        } else {
            try {
                // convert to a number, and if it is not 0 then it is true
                BigDecimal(s).signum() != 0
            } catch (e: NumberFormatException) {
                throw getDataConversionError(BOOLEAN)
            }
        }
    }

    override fun getByte(): Byte = try {
        value.trim { it <= ' ' }.toByte()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getShort(): Short = try {
        value.trim { it <= ' ' }.toShort()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getInt(): Int = try {
        value.trim { it <= ' ' }.toInt()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getLong(): Long = try {
        value.trim { it <= ' ' }.toLong()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getBigDecimal(): BigDecimal = try {
        BigDecimal(value.trim { it <= ' ' })
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getFloat(): Float = try {
        value.trim { it <= ' ' }.toFloat()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getDouble(): Double = try {
        value.trim { it <= ' ' }.toDouble()
    } catch (e: NumberFormatException) {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, value)
    }

    override fun getMemory(): Int {
        /*
         * Java 11 with -XX:-UseCompressedOops
         * Empty string: 88 bytes
         * 1 to 4 UTF-16 chars: 96 bytes
         */
        return value.length * 2 + 94
    }

    override fun equals(other: Any?): Boolean = other != null
            && javaClass == other.javaClass
            && value == (other as ValueStringBase).value
}