package org.h2.api

import org.h2.message.DbException
import java.lang.IllegalArgumentException

/**
 * INTERVAL representation for result sets.
 */
/**
 * Creates a new interval. Do not use this constructor, use static methods instead.
 * @param qualifier qualifier
 * @param negative whether interval is negative
 * @param leading value of leading field
 * @param remaining combined value of all remaining fields
 */
class Interval(val qualifier: IntervalQualifier,
               val leading: Long,
               val remaining: Long,
               negative: Boolean) {
    var negative:Boolean = false
    init {
        try {
            this.negative = IntervalU
        } catch (e: DbException) {
            throw IllegalArgumentException(e)
        }
    }

    companion object {

    }
}