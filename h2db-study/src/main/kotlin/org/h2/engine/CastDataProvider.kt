package org.h2.engine

import org.h2.api.JavaObjectSerializer
import org.h2.util.TimeZoneProvider
import org.h2.value.ValueTimestampTimeZone

/**
 * Provides information for type casts and comparison operations.
 */
interface CastDataProvider {
    /**
     * Returns the current timestamp with maximum resolution. The value must be
     * the same within a transaction or within execution of a command.
     *
     * @return the current timestamp for CURRENT_TIMESTAMP(9)
     */
    fun currentTimestamp(): ValueTimestampTimeZone

    /**
     * Returns the current time zone.
     *
     * @return the current time zone
     */
    fun currentTimeZone(): TimeZoneProvider

    /**
     * Returns the database mode.
     *
     * @return the database mode
     */
    fun getMode(): Mode

    /**
     * Returns the custom Java object serializer, or `null`.
     *
     * @return the custom Java object serializer, or `null`
     */
    fun getJavaObjectSerializer(): JavaObjectSerializer?

    /**
     * Returns are ENUM values 0-based.
     *
     * @return are ENUM values 0-based
     */
    fun zeroBasedEnums(): Boolean
}