package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.message.DbException
import org.h2.util.HasSQL
import org.h2.util.StringUtils
import org.h2.util.Utils

/**
 * Implementation of the JAVA_OBJECT data type.
 */
class ValueJavaObject(v: ByteArray) : ValueBytesBase(v) {
    companion object {
        private val EMPTY = ValueJavaObject(Utils.EMPTY_BYTES)

        /**
         * Get or create a java object value for the given byte array.
         * Do not clone the data.
         *
         * @param b the byte array
         * @return the value
         */
        fun getNoCopy(b: ByteArray): ValueJavaObject {
            if (b.isEmpty()) return EMPTY
            val length = b.size
            val obj = ValueJavaObject(b)
            return if (length > SysProperties.OBJECT_CACHE_MAX_PER_ELEMENT_SIZE) {
                obj
            } else cache(obj) as ValueJavaObject
        }

        /**
         * Converts this value to a JAVA_OBJECT value. May not be called on a NULL
         * value.
         *
         * @param targetType
         * the type of the returned value
         * @param conversionMode
         * conversion mode
         * @param column
         * the column (if any), used to improve the error message if
         * conversion fails
         * @return the JAVA_OBJECT value
         */
        fun Value.convertToJavaObject(targetType: TypeInfo, conversionMode: Int, column: Any?): ValueJavaObject {
            val v: ValueJavaObject = when (getValueType()) {
                JAVA_OBJECT -> this as ValueJavaObject
                BINARY, VARBINARY, BLOB -> getNoCopy(getBytesNoCopy())
                NULL -> throw DbException.getInternalError()
                else -> throw getDataConversionError(JAVA_OBJECT)
            }
            if (conversionMode != CONVERT_TO && v.getBytesNoCopy().size > targetType.precision) {
                throw v.getValueTooLongException(targetType, column)
            }
            return v
        }
    }

    init {
        if (v.size > Constants.MAX_STRING_LENGTH)
            throw DbException.getValueTooLongException(getTypeName(getValueType()),
                    StringUtils.convertBytesToHex(value, 41), v.size.toLong())
    }

    override val type: TypeInfo = TypeInfo.TYPE_JAVA_OBJECT

    override fun getValueType(): Int = JAVA_OBJECT

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return if (sqlFlags and HasSQL.NO_CASTS == 0) {
            super.getSQL(builder.append("CAST("), HasSQL.DEFAULT_SQL_FLAGS).append(" AS JAVA_OBJECT)")
        } else super.getSQL(builder, HasSQL.DEFAULT_SQL_FLAGS)
    }

    override fun getString(): String? {
        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, "JAVA_OBJECT to CHARACTER VARYING")
    }
}