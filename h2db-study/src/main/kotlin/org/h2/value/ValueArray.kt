package org.h2.value

import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException
import org.h2.util.MathUtils
import java.util.Arrays

/**
 * Implementation of the ARRAY data type.
 */
class ValueArray(val componentType: TypeInfo, list: Array<Value?>, provider: CastDataProvider) : ValueCollectionBase(list) {

    init {
        val length: Int = list.size
        if (length > Constants.MAX_ARRAY_CARDINALITY) {
            val typeName = getTypeName(getValueType())
            throw DbException.getValueTooLongException(typeName, typeName, length.toLong())
        }
        for (i in 0 until length) {
            list[i] = list[i]!!.castTo(componentType, provider)
        }
    }

    companion object {
        /**
         * Get or create a array value for the given value array.
         * Do not clone the data.
         *
         * @param list the value array
         * @param provider the cast information provider
         * @return the value
         */
        fun get(list: Array<Value?>, provider: CastDataProvider?): ValueArray {
            return ValueArray(TypeInfo.getHigherType(list as Array<Typed?>)!!, list, provider!!)
        }

        /**
         * Get or create a array value for the given value array.
         * Do not clone the data.
         *
         * @param componentType the type of elements, or `null`
         * @param list the value array
         * @param provider the cast information provider
         * @return the value
         */
        operator fun get(componentType: TypeInfo?,
                         list: Array<Value?>?,
                         provider: CastDataProvider?): ValueArray = ValueArray(componentType!!, list!!, provider!!)

        fun Value.convertToArray(targetType: TypeInfo,
                                 provider: CastDataProvider?,
                                 conversionMode: Int,
                                 column: Any?): ValueArray {

            val componentType: TypeInfo = targetType.extTypeInfo as TypeInfo
            val valueType: Int = getValueType()

            if (valueType == ARRAY) return this as ValueArray

            var v: ValueArray = get(list = when (valueType) {
                BLOB -> arrayOf<Value?>(ValueVarbinary[getBytesNoCopy()])
                CLOB -> arrayOf(ValueVarchar.get(getString()!!))
                else -> arrayOf(this)
            }, provider = provider)

            if (componentType != null) {
                val values = v.getList()
                val length = values!!.size
                var i = 0
                loop@ while (i < length) {
                    val v1 = values[i]
                    val v2 = v1!!.convertTo(componentType, provider, conversionMode, column)
                    if (v1 !== v2) {
                        val newValues = arrayOfNulls<Value>(length)
                        System.arraycopy(values, 0, newValues, 0, i)
                        newValues[i] = v2
                        while (++i < length) {
                            newValues[i] = values[i]!!.convertTo(componentType, provider, conversionMode, column)
                        }
                        v = ValueArray[componentType, newValues, provider]
                        break@loop
                    }
                    i++
                }
            }

            if (conversionMode != CONVERT_TO) {
                val values = v.getList()
                val cardinality = values!!.size
                if (conversionMode == CAST_TO) {
                    val p = MathUtils.convertLongToInt(targetType.precision)
                    if (cardinality > p) {
                        v = ValueArray[v.componentType, Arrays.copyOf(values, p), provider]
                    }
                } else if (cardinality > targetType.precision) {
                    throw v.getValueTooLongException(targetType, column)
                }
            }
            return v
        }

        /**
         * Convert this value to any ARRAY data type.
         *
         * @param provider the cast information provider
         * @return a row value
         */
        fun Value.convertToAnyArray(provider: CastDataProvider?): ValueArray = if (getValueType() == ARRAY)
            this as ValueArray
        else ValueArray[this.getType(), arrayOf(this), provider]


    }


    override var type: TypeInfo? = null
        get() {
            if (field == null)
                field = TypeInfo.getTypeInfo(getValueType(), values!!.size.toLong(), 0, componentType)
            return field
        }


    override fun getValueType(): Int = ARRAY

    override fun getString(): String = values!!.joinToString(prefix = "[", separator = ", ", postfix = "]") { it!!.getString()!! }


    override fun compareTypeSafe(o: Value, mode: CompareMode?, provider: CastDataProvider?): Int {
        val v = o as ValueArray
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

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = values.joinTo(
            buffer = StringBuilder(),
            prefix = "ARRAY [",
            separator = ", ",
            postfix = "]") { it!!.getSQL(builder, sqlFlags) }

    override fun equals(other: Any?): Boolean = other is ValueArray && values.contentEquals(other.values)

}