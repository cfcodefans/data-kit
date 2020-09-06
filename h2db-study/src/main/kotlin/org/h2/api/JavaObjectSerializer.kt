package org.h2.api

/**
 * Custom serialization mechanism for java objects being stored in column of
 * type OTHER.
 */
interface JavaObjectSerializer {
    /**
     * Serialize object to byte array.
     * @param obj the object to serialize
     * @return the byte array of the serialized object
     */
    @Throws(Exception::class)
    fun serialize(obj: Any?): ByteArray

    /**
     * Deserialize object from byte array.
     * @param bytes the byte array of the serialized object
     * @return the object
     */
    @Throws(Exception::class)
    fun deserialize(bytes: ByteArray)
}