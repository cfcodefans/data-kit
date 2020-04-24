package org.h2.util

import org.h2.api.ErrorCode
import org.h2.api.JavaObjectSerializer
import org.h2.message.DbException
import org.h2.store.DataHandler

/**
 * This is a utility class with JDBC helper functions.
 */
object JdbcUtils {
    /**
     * The serializer to use.
     */
    lateinit var serializer: JavaObjectSerializer

    /**
     *
     */
    @JvmStatic
    fun serialize(obj: Any, dataHandler: DataHandler?): ByteArray {
        try {
            dataHandler?.
        } catch (e: Throwable) {
            throw DbException.get(ErrorCode.SERIALIZATION_FAILED_1, e, e.toString())
        }
    }
}