package org.h2.engine

/**
 * Constants are fixed values that are used in the whole database coe.
 */
object Constants {
    /**
     * The build date is updated for each public release.
     */
    val BUILD_DATE: String = "2019-03-13"

    /**
     * The minor version of this database.
     */
    const val VERSION_MINOR: Int = 4
    /**
     * The major version of this database.
     */
    const val VERSION_MAJOR: Int = 1
    /**
     * The database URL prefix of this database.
     */
    const val START_URL: String = "jdbc:h2:"

    /**
     * The block size for I/O operations.
     */
    const val IO_BUFFER_SIZE: Int = 8 * 1024
}