package org.h2.value

/**
 * Implementation of NULL. NULL is not a regular data type.
 */
class ValueNull : Value {
    companion object {
        /**
         * The main NULL instance.
         */
        @JvmStatic
        val INSTANCE: ValueNull = ValueNull()
        /**
         * The precision of NULL
         */
        const val PRECISION: Int = 1
        /**
         * The display size of the textual representation of NULL.
         */
        const val DISPLAY_SIZE: Int = 4
    }
}