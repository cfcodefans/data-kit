package org.h2.value

import org.h2.api.ErrorCode
import org.h2.engine.CastDataProvider
import org.h2.engine.Constants
import org.h2.message.DbException

/**
 * Extended parameters of the ENUM data type.
 */
class ExtTypeInfoEnum(private val enumerators: Array<String?>) : ExtTypeInfo() {

    companion object {
        private fun sanitize(label: String?): String? {
            if (label == null) return null

            val length = label.length
            if (length > Constants.MAX_STRING_LENGTH) {
                throw DbException.getValueTooLongException("ENUM", label, length.toLong())
            }
            return label.trim { it <= ' ' }.uppercase()
        }

        private fun toSQL(builder: StringBuilder, enumerators: Array<String?>): StringBuilder {
            builder.append('(')

            for (i in enumerators.indices) {
                if (i != 0) builder.append(", ")
                if (enumerators[i].isNullOrBlank()) continue

                builder.append('\'')
                for (c in enumerators[i]!!) {
                    if (c == '\'') builder.append('\'')
                    builder.append(c)
                }
                builder.append('\'')
            }
            return builder.append(')')
        }

        /**
         * Returns enumerators for the two specified values for a binary operation.
         *
         * @param left left (first) operand
         * @param right right (second) operand
         * @return enumerators from the left or the right value, or an empty array
         * if both values do not have enumerators
         */
        fun getEnumeratorsForBinaryOperation(left: Value, right: Value): ExtTypeInfoEnum = when (Value.ENUM) {
            left.getValueType() -> (left as ValueEnum).enumerators
            right.getValueType() -> (right as ValueEnum).enumerators
            else -> throw DbException.get(ErrorCode.UNKNOWN_DATA_TYPE_1, "type1=${left.getValueType()}, type2=${right.getValueType()}")
        }
    }

    private lateinit var cleaned: Array<String?>

    init {
        if (enumerators.isEmpty()) throw DbException.get(ErrorCode.ENUM_EMPTY)

        val length: Int = enumerators.size
        if (length > Constants.MAX_ARRAY_CARDINALITY) {
            throw DbException.getValueTooLongException("ENUM", "($length elements)", length.toLong())
        }

        val cleaned = arrayOfNulls<String>(length)
        enumerators.forEachIndexed() { i, s ->
            val l: String? = sanitize(s)
            if (l.isNullOrBlank()) throw DbException.get(ErrorCode.ENUM_EMPTY)

            if ((0..i).any { cleaned[it] == s })
                throw DbException.get(ErrorCode.ENUM_DUPLICATE, toSQL(StringBuilder(), enumerators).toString())
            cleaned[i] = l
        }
        this.cleaned = if (cleaned.contentEquals(enumerators)) enumerators else cleaned
    }

    val type: TypeInfo by lazy {
        TypeInfo(valueType = Value.ENUM,
                precision = enumerators.maxOf { it!!.length }.toLong(),
                scale = 0,
                extTypeInfo = this)
    }

    /**
     * Get count of elements in enumeration.
     *
     * @return count of elements in enumeration
     */
    fun getCount(): Int = enumerators.size

    /**
     * Returns an enumerator with specified 0-based ordinal value.
     *
     * @param ordinal
     * ordinal value of an enumerator
     * @return the enumerator with specified ordinal value
     */
    fun getEnumerator(ordinal: Int): String? = enumerators[ordinal]

    /**
     * Get ValueEnum instance for an ordinal.
     * @param ordinal ordinal value of an enum
     * @param provider the cast information provider
     * @return ValueEnum instance
     */
    fun getValue(ordinal: Int, provider: CastDataProvider?): ValueEnum {
        val startIdx: Int = if (provider != null && provider.zeroBasedEnums()) 0 else 1

        if (ordinal < startIdx || ordinal >= enumerators.size)
            throw DbException.get(ErrorCode.ENUM_VALUE_NOT_PERMITTED, getTraceSQL()!!, ordinal.toString())

        return ValueEnum(this, enumerators[ordinal - startIdx]!!, ordinal)
    }

    /**
     * Get ValueEnum instance for a label string.
     * @param label label string
     * @param provider the cast information provider
     * @return ValueEnum instance
     */
    fun getValue(label: String?, provider: CastDataProvider?): ValueEnum {
        return getValueOrNull(label, provider)
                ?: throw DbException.get(ErrorCode.ENUM_VALUE_NOT_PERMITTED, toString(), label!!)
    }

    private fun getValueOrNull(label: String?, provider: CastDataProvider?): ValueEnum? {
        val l = sanitize(label) ?: return null

        val ordinal = if (provider != null && provider.zeroBasedEnums()) 0 else 1
        for (i in cleaned.indices) {
            if (l == cleaned[i]) return ValueEnum(this, enumerators[i]!!, i + ordinal)
        }

        return null
    }

    override fun hashCode(): Int = enumerators.contentHashCode() + 203117

    override fun equals(obj: Any?): Boolean {
        return (this === obj)
                || (obj?.javaClass == ExtTypeInfoEnum::class.java)
                && enumerators.contentEquals((obj as ExtTypeInfoEnum).enumerators)
    }

    override fun getSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder = toSQL(builder, enumerators)
}