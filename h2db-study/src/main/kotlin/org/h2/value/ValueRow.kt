package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException


/**
 * Row value.
 */
class ValueRow private constructor(type: TypeInfo?, list: Array<Value?>) : ValueCollectionBase(list) {
    override var type: TypeInfo? = null
        get() {
            if (field == null) {
                field = TypeInfo.getTypeInfo(ROW, 0, 0, ExtTypeInfoRow(values as Array<Typed>))
            }
            return field
        }

    init {
        val degree = list.size
        if (degree > Constants.MAX_COLUMNS) {
            throw DbException.get(ErrorCode.TOO_MANY_COLUMNS_1, "" + Constants.MAX_COLUMNS)
        }
        if (type != null) {
            if (type.valueType != ROW || (type.extTypeInfo as ExtTypeInfoRow).getFields().size != degree)
                throw DbException.getInternalError()
            this.type = type
        }
    }

    companion object {
        /**
         * Get or create a row value for the given value array.
         * Do not clone the data.
         *
         * @param list the value array
         * @return the value
         */
        operator fun get(list: Array<Value?>?): ValueRow = ValueRow(null, list!!)

        /**
         * Get or create a typed row value for the given value array.
         * Do not clone the data.
         *
         * @param extTypeInfo the extended data type information
         * @param list the value array
         * @return the value
         */
        operator fun get(extTypeInfo: ExtTypeInfoRow?, list: Array<Value?>?): ValueRow =
                ValueRow(TypeInfo(ROW, -1, -1, extTypeInfo), list!!)

        /**
         * Get or create a typed row value for the given value array.
         * Do not clone the data.
         *
         * @param targetType the data type information
         * @param list the value array
         * @return the value
         */
        operator fun get(targetType: TypeInfo?, list: Array<Value?>?): ValueRow = ValueRow(targetType, list!!)

        fun Value.convertToRow(targetType: TypeInfo, provider: CastDataProvider?, conversionMode: Int, column: Any?): ValueRow {
            var vr: ValueRow = if (getValueType() == ROW) this as ValueRow else ValueRow.get(arrayOf(this))

            val ext: ExtTypeInfoRow? = targetType.extTypeInfo as ExtTypeInfoRow?
            if (ext != null) {
                val values: Array<Value?> = vr.values
                val len: Int = values.size
                val fields = ext.getFields()
                if (len != fields.size) throw getDataConversionError(targetType)

                val iter = fields.iterator()
                var i = 0 //TODO, ugly and cumbersome
                loop@ while (i < len) {
                    val v1 = values[i]!!
                    val componentType = iter.next().value
                    val v2 = v1.convertTo(componentType, provider, conversionMode, column)
                    if (v1 !== v2) {
                        val newValues = arrayOfNulls<Value>(len)
                        System.arraycopy(values, 0, newValues, 0, i)
                        newValues[i] = v2
                        while (++i < len) {
                            newValues[i] = values[i]!!.convertTo(componentType, provider, conversionMode, column)
                        }
                        vr = ValueRow[targetType, newValues]
                        break@loop
                    }
                    i++
                }
            }

            return vr
        }

        /**
         * Convert this value to any ROW data type.
         *
         * @return a row value
         */
        fun Value.convertToAnyRow(): ValueRow = if (getValueType() == ROW)
            this as ValueRow
        else ValueRow[arrayOf(this)]
    }

    override fun getValueType(): Int = ROW

    override fun getString(): String = values.joinToString(prefix = "ROW (", separator = ", ", postfix = ")") { it!!.getString()!! }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = values.joinTo(
            buffer = StringBuilder(),
            prefix = "ROW (",
            separator = ", ",
            postfix = ")") { it!!.getSQL(builder, sqlFlags) }

    override fun equals(other: Any?): Boolean = other is ValueRow && values.contentEquals(other.values)

    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val v = o as ValueRow
        if (values === v.values) return 0

        val l = values!!.size
        val ol = v.values!!.size
        val len = l.coerceAtMost(ol)
        for (i in 0 until len) {
            val v1 = values[i]!!
            val v2 = v.values[i]!!
            val comp = v1.compareTo(v2, provider, mode)
            if (comp != 0) return comp
        }
        return l.compareTo(ol)
    }
}