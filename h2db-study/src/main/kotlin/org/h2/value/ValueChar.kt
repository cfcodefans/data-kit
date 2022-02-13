package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.engine.Mode.CharPadding
import org.h2.engine.SysProperties
import org.h2.util.HasSQL
import org.h2.util.MathUtils
import org.h2.util.StringUtils

/**
 * Implementation of the CHARACTER data type.
 */
class ValueChar(value: String) : ValueStringBase(value) {

    companion object {
        @Deprecated("need to clean up, the logic is so messy")
        internal fun Value.convertToChar(targetType: TypeInfo,
                                         provider: CastDataProvider?,
                                         conversionMode: Int,
                                         column: Any?): ValueChar {
            val valueType = getValueType()
            if (valueType == BLOB || valueType == JAVA_OBJECT) throw getDataConversionError(targetType.valueType)

            var s = getString()
            val length = s!!.length
            var newLength = length

            if (conversionMode == CONVERT_TO) {
                while (newLength > 0 && s[newLength - 1] == ' ') {
                    newLength--
                }
            } else {
                val p = MathUtils.convertLongToInt(targetType.precision)
                if (provider == null || provider.getMode().charPadding == CharPadding.ALWAYS) {
                    if (newLength != p) {
                        if (newLength < p) return ValueChar[StringUtils.pad(s, p, null, true)!!]!!

                        if (conversionMode == CAST_TO) {
                            newLength = p
                        } else {
                            do {
                                if (s[--newLength] != ' ') throw getValueTooLongException(targetType, column)
                            } while (newLength > p)
                        }
                    }
                } else {
                    if (conversionMode == CAST_TO && newLength > p) {
                        newLength = p
                    }
                    while (newLength > 0 && s[newLength - 1] == ' ') {
                        newLength--
                    }
                    if (conversionMode == ASSIGN_TO && newLength > p) {
                        throw getValueTooLongException(targetType, column)
                    }
                }
            }
            if (length != newLength) {
                s = s.substring(0, newLength)
            } else if (valueType == CHAR) {
                return (this as ValueChar)
            }
            return ValueChar[s]!!
        }

        open fun Value.convertToChar(): ValueChar = convertToChar(
                targetType = TypeInfo.getTypeInfo(CHAR),
                provider = null,
                conversionMode = CONVERT_TO,
                column = null)

        /**
         * Get or create a CHAR value for the given string.
         *
         * @param s the string
         * @return the value
         */
        operator fun get(s: String): ValueChar? {
            val obj = ValueChar(StringUtils.cache(s)!!)
            return if (s.length > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE)
                obj
            else cache(obj) as ValueChar
        }
    }

    override fun getValueType(): Int = CHAR

    override fun compareTypeSafe(v: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        return mode!!.compareString(convertToChar().getString(), v.convertToChar().getString(), false)
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        if (sqlFlags and HasSQL.NO_CASTS != 0) return StringUtils.quoteStringSQL(builder, value)

        val length = value.length
        return StringUtils.quoteStringSQL(builder.append("CAST("), value)
                .append(" AS CHAR(")
                .append(if (length > 0) length else 1)
                .append("))")
    }
}