package org.h2.value.lob

import org.h2.store.DataHandler
import org.h2.value.ValueLob
import java.io.InputStream

/**
 * LOB data.
 */
abstract class LobData {

    /**
     * Get stream to read LOB data from
     * @param precision octet length of the stream, or -1 if unknown
     * @return stream to read LOB data from
     */
    abstract fun getInputStream(precision: Long): InputStream?

    open fun getDataHandler(): DataHandler? = null

    open fun isLinkedToTable(): Boolean = false

    /**
     * Remove the underlying resource, if any. For values that are kept fully in
     * memory this method has no effect.
     * @param value to remove
     */
    open fun remove(value: ValueLob?) {}

    /**
     * Get the memory used by this object.
     *
     * @return the memory used in bytes
     */
    open fun getMemory(): Int = 140
}