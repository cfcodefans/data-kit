package org.h2.api

/**
 * Interval qualifier
 */
enum class IntervalQualifier {
    /**
     * {@code YEAR}
     */
    YEAR,
    /**
     * {@code MONTH}
     */
    MONTH,
    /**
     * {@code DAY}
     */
    DAY,
    /**
     * {@code HOUR}
     */
    HOUR,
    /**
     * {@code MINUTE}
     */
    MINUTE,
    /**
     * {@code SECOND}
     */
    SECOND,
    /**
     * {@code YEAR TO MONTH}
     */
    YEAR_TO_MONTH,
    /**
     * {@code DAY TO HOUR}
     */
    DAY_TO_HOUR,
    /**
     * {@code DAY TO MINUTE}
     */
    DAY_TO_MINUTE,
    /**
     * {@code DAY TO SECOND}
     */
    DAY_TO_SECOND,
    /**
     * {@code HOUR TO MINUTE}
     */
    HOUR_TO_MINUTE,
    /**
     * {@code HOUR TO SECOND}
     */
    HOUR_TO_SECOND,
    /**
     * {@code MINUTE TO SECOND}
     */
    MINUTE_TO_SECOND,
}