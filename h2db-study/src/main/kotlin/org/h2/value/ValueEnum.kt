package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.message.DbException
import org.h2.util.HasSQL
import org.h2.util.StringUtils

/**
 * ENUM value.
 */
class ValueEnum(val enumerators: ExtTypeInfoEnum, label: String, ordinal: Int) : ValueEnumBase(label, ordinal) {
    companion object {
        /**
         * Converts this value to an ENUM value. May not be called on a NULL value.
         *
         * @param extTypeInfo the extended data type information
         * @param provider the cast information provider
         * @return the ENUM value
         */
        fun Value.convertToEnum(extTypeInfo: ExtTypeInfoEnum, provider: CastDataProvider?): ValueEnum = when (getValueType()) {
            ENUM -> {
                val v = this as ValueEnum
                if (extTypeInfo == v.enumerators) v else extTypeInfo.getValue(v.getString(), provider)
            }
            TINYINT, SMALLINT, INTEGER, BIGINT, NUMERIC, DECFLOAT -> extTypeInfo.getValue(getInt(), provider)
            VARCHAR, VARCHAR_IGNORECASE, CHAR -> extTypeInfo.getValue(getString(), provider)
            NULL -> throw DbException.getInternalError()
            else -> throw getDataConversionError(ENUM)
        }
    }

    override val type: TypeInfo
        get() = enumerators.type

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        if (sqlFlags and HasSQL.NO_CASTS == 0) {
            StringUtils.quoteStringSQL(builder.append("CAST("), super.label).append(" AS ")
            return enumerators.type.getSQL(builder, sqlFlags).append(')')
        }
        return StringUtils.quoteStringSQL(builder, super.label)
    }
}