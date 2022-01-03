package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.util.Bits
import org.h2.util.StringUtils
import org.h2.util.Utils

/**
 * Base implementation of byte array based data types.
 */
abstract class ValueBytesBase(var value: ByteArray) : Value() {
    /**
     * The hash code.
     */
    var hash = 0

    override fun hashCode(): Int {
        if (hash != 0) return hash

        val h = javaClass.hashCode() xor Utils.getByteArrayHash(value)
        hash = if (h == 0) 1234570417 else h
        return hash
    }

    override fun getBytes(): ByteArray = Utils.cloneByteArray(value)!!

    override fun getBytesNoCopy(): ByteArray = value

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return Bits.compareNotNullUnsigned(value, (v as ValueBytesBase).value)
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return StringUtils.convertBytesToHex(builder.append("X'"), value)!!.append('\'')
    }

    override fun getMemory(): Int = value.size + 24

    override fun equals(other: Any?): Boolean = other != null
            && javaClass == other.javaClass
            && value.contentEquals((other as ValueBytesBase).value)
}