package org.h2.value.lob

import org.h2.engine.SessionRemote
import org.h2.store.LobStorageRemoteInputStream
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * A implementation of the LOB data used on the client side of a remote H2
 * connection. Fetches the underlying on data from the server.
 */
class LobDataFetchOnDemand(var handler: SessionRemote?,
                           val tableId: Int,
                           val lobId: Long,
                           val hmac: ByteArray) : LobData() {

    /**
     * Check if this value is linked to a specific table. For values that are
     * kept fully in memory, this method returns false.
     *
     * @return true if it is
     */
    override fun isLinkedToTable(): Boolean = throw IllegalStateException()

    override fun getInputStream(precision: Long): InputStream =
            BufferedInputStream(LobStorageRemoteInputStream(handler, lobId, hmac))

    override fun toString(): String = "lob-table: table: $tableId id: $lobId"
}