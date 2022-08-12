package org.h2.store.fs

/**
 * A recorder for the recording file system.
 */
interface Recorder {
    companion object {
        /**
         * Create a new file.
         */
        const val CREATE_NEW_FILE = 2

        /**
         * Create a temporary file.
         */
        const val CREATE_TEMP_FILE = 3

        /**
         * Delete a file.
         */
        const val DELETE = 4

        /**
         * Open a file output stream.
         */
        const val OPEN_OUTPUT_STREAM = 5

        /**
         * Rename a file. The file name contains the source and the target file
         * separated with a colon.
         */
        const val RENAME = 6

        /**
         * Truncate the file.
         */
        const val TRUNCATE = 7

        /**
         * Write to the file.
         */
        const val WRITE = 8
    }

    /**
     * Record the method.
     *
     * @param op the operation
     * @param fileName the file name or file name list
     * @param data the data or null
     * @param x the value or 0
     */
    fun log(op: Int, fileName: String?, data: ByteArray?, x: Long)
}