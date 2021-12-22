package org.h2.value.lob

import org.h2.message.DbException
import org.h2.store.DataHandler
import org.h2.value.ValueLob
import java.io.IOException
import java.io.InputStream

/**
 * LOB data stored in database.
 */
class LobDataDatabase(var handler: DataHandler?,
                      val tableId: Int,
                      val lobId: Long) : LobData() {

    /**
     * Fix for recovery tool.
     */
    var isRecoveryReference = false

    override fun remove(value: ValueLob?) {
        handler?.getLobStorage()?.removeLob(value)
    }

    /**
     * Check if this value is linked to a specific table. For values that are
     * kept fully in memory, this method returns false.
     *
     * @return true if it is
     */
    override fun isLinkedToTable(): Boolean = tableId >= 0

    override fun getInputStream(precision: Long): InputStream? = try {
        handler!!.getLobStorage().getInputStream(lobId, tableId, precision)
    } catch (e: IOException) {
        throw DbException.convertIOException(e, toString())
    }

    override fun toString(): String = "lob-table: table: $tableId id: $lobId"
}