package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.engine.SysProperties
import org.h2.util.MathUtils
import org.h2.util.StringUtils

/**
 * Implementation of the CHARACTER VARYING data type.
 */
class ValueVarchar(value: String) : ValueStringBase(value) {
    companion object {
        /**
         * Empty string. Should not be used in places where empty string can be
         * treated as `NULL` depending on database mode.
         */
        val EMPTY = ValueVarchar("")

        /**
         * Get or create a VARCHAR value for the given string.
         *
         * @param s the string
         * @return the value
         */
        fun get(s: String): Value = get(s, null)

        /**
         * Get or create a VARCHAR value for the given string.
         *
         * @param s the string
         * @param provider the cast information provider, or `null`
         * @return the value
         */
        fun get(s: String, provider: CastDataProvider?): Value {
            if (s.isEmpty()) {
                return if (provider?.getMode()?.treatEmptyStringsAsNull == true) ValueNull.INSTANCE else EMPTY
            }
            val obj = ValueVarchar(StringUtils.cache(s)!!)
            return if (s.length > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
                obj
            } else cache(obj)
            // this saves memory, but is really slow
            // return new ValueString(s.intern());
        }

        fun Value.convertToVarchar(targetType: TypeInfo, provider: CastDataProvider?, conversionMode: Int, column: Any?): Value {
            val valueType = getValueType()
            when (valueType) {
                BLOB, JAVA_OBJECT -> throw getDataConversionError(targetType.valueType)
            }

            if (conversionMode != CONVERT_TO) {
                val s = getString()
                val p = MathUtils.convertLongToInt(targetType.precision)
                if (s!!.length > p) {
                    if (conversionMode != CAST_TO) throw getValueTooLongException(targetType, column)
                    return get(s.substring(0, p), provider)
                }
            }
            return if (valueType == VARCHAR) this as ValueVarchar else get(getString()!!, provider)
        }

        fun Value.convertToVarcharIgnoreCase(targetType: TypeInfo, conversionMode: Int, column: Any?): Value {
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
    }

    override fun getValueType(): Int = VARCHAR

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = StringUtils.quoteStringSQL(builder, value)


}