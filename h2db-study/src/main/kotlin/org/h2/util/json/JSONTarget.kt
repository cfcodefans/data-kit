package org.h2.util.json

import java.math.BigDecimal

/**
 * Abstract JSON output target.
 *
 * @param <R> the type of the result
 */
abstract class JSONTarget<R> {
    /**
     * Start of an object.
     */
    abstract fun startObject()

    /**
     * End of the current object.
     */
    abstract fun endObject()

    /**
     * Start of an array.
     */
    abstract fun startArray()

    /**
     * End of the current array.
     */
    abstract fun endArray()

    /**
     * Name of a member.
     * @param name the name
     */
    abstract fun member(name: String?)

    /**
     * Parse "null".
     * `null` value.
     */
    abstract fun valueNull()

    /**
     * Parse "false".
     * `false` value.
     */
    abstract fun valueFalse()

    /**
     * Parse "true".
     * `true` value.
     */
    abstract fun valueTrue()

    /**
     * A number value.
     *
     * @param number the number
     */
    abstract fun valueNumber(number: BigDecimal?)

    /**
     * A string value.
     * @param string the string
     */
    abstract fun valueString(string: String?)

    /**
     * Returns whether member's name or the end of the current object is
     * expected.
     * @return `true` if it is, `false` otherwise
     */
    abstract fun isPropertyExpected(): Boolean

    /**
     * Returns whether value separator expected before the next member or value.
     * @return `true` if it is, `false` otherwise
     */
    abstract fun isValueSeparatorExpected(): Boolean

    /**
     * Returns the result.
     * @return the result
     */
    abstract fun getResult(): R?
}