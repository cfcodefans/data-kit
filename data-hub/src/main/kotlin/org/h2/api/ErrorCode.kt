package org.h2.api

object ErrorCode {
    /**
     * The error with code `90108` is thrown when not enough heap
     * memory was available. A possible solutions is to increase the memory size
     * using `java -Xmx128m ...`. Another solution is to reduce
     * the cache size.
     */
    const val OUT_OF_MEMORY:Int = 90108
}