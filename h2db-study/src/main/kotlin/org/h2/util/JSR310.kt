package org.h2.util

object JSR310 {
    /**
     * `Class<java.time.LocalDate>` or `null`.
     */
    var LOCAL_DATE: Class<*>? = null

    /**
     * `Class<java.time.LocalTime>` or `null`.
     */
    var LOCAL_TIME: Class<*>? = null

    /**
     * `Class<java.time.LocalDateTime>` or `null`.
     */
    var LOCAL_DATE_TIME: Class<*>? = null

    /**
     * `Class<java.time.Instant>` or `null`.
     */
    var INSTANT: Class<*>? = null

    /**
     * `Class<java.time.OffsetDateTime>` or `null`.
     */
    var OFFSET_DATE_TIME: Class<*>? = null

    /**
     * `Class<java.time.ZonedDateTime>` or `null`.
     */
    var ZONED_DATE_TIME: Class<*>? = null

    /**
     * `Class<java.time.OffsetTime>` or `null`.
     */
    var OFFSET_TIME: Class<*>? = null

    /**
     * `Class<java.time.Period>` or `null`.
     */
    var PERIOD: Class<*>? = null

    /**
     * `Class<java.time.Duration>` or `null`.
     */
    var DURATION: Class<*>? = null

    /**
     * Whether the JSR 310 date and time API present in the JRE.
     */
    var PRESENT = false

    init {
        var present = false
        var localDate: Class<*>? = null
        var localTime: Class<*>? = null
        var localDateTime: Class<*>? = null
        var instant: Class<*>? = null
        var offsetDateTime: Class<*>? = null
        var zonedDateTime: Class<*>? = null
        var offsetTime: Class<*>? = null
        var period: Class<*>? = null
        var duration: Class<*>? = null
        try {
            localDate = Class.forName("java.time.LocalDate")
            localTime = Class.forName("java.time.LocalTime")
            localDateTime = Class.forName("java.time.LocalDateTime")
            instant = Class.forName("java.time.Instant")
            offsetDateTime = Class.forName("java.time.OffsetDateTime")
            zonedDateTime = Class.forName("java.time.ZonedDateTime")
            offsetTime = Class.forName("java.time.OffsetTime")
            period = Class.forName("java.time.Period")
            duration = Class.forName("java.time.Duration")
            present = true
        } catch (t: Throwable) {
            // Ignore
        }
        LOCAL_DATE = localDate
        LOCAL_TIME = localTime
        LOCAL_DATE_TIME = localDateTime
        INSTANT = instant
        OFFSET_DATE_TIME = offsetDateTime
        ZONED_DATE_TIME = zonedDateTime
        OFFSET_TIME = offsetTime
        PERIOD = period
        DURATION = duration
        PRESENT = present
    }
}