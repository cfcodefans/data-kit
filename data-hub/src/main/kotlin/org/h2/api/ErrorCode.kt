package org.h2.api

/**
 * This class defines the error codes used for SQL exceptions.
 * Error messages are formatted as follows:
 * <pre>
 *     { error message (possibly translated; may include quoted data) }
 *     { error message in English if different }
 *     { SQL statement if applicable }
 *     { [ error code - build number ] }
 *  </pre>
 *  Example:
 *  <pre>
 *      Syntax error in SQL statement "SELECT * FORM[*] TEST ";
 *      SQL statement: select * form test [42000-125]
 *  </pre>
 *  The [*] marks the position of the syntax error
 *  (FORM instead of FROM in the case).
 *  The error code is 42000, and the build number is 125,
 *  meaning version 1.2.125.
 */
object ErrorCode {
    // 02: no data
    /**
     * The error with code `90108` is thrown when not enough heap
     * memory was available. A possible solutions is to increase the memory size
     * using `java -Xmx128m ...`. Another solution is to reduce
     * the cache size.
     */
    const val OUT_OF_MEMORY: Int = 90108
}