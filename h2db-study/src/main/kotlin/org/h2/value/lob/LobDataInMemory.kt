package org.h2.value.lob

import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * LOB data stored in memory.
 * If the LOB is below the inline size, we just store/load it directly here.
 */
class LobDataInMemory(val small: ByteArray) : LobData() {

    /**
     * Java 11 with -XX:-UseCompressedOops 0 bytes: 120 bytes 1 byte: 128 bytes
     */
    override fun getMemory(): Int = small.size + 127

    override fun getInputStream(precision: Long): InputStream = ByteArrayInputStream(small)

}