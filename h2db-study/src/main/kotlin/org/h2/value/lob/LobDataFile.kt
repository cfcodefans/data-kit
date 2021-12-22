package org.h2.value.lob

import org.h2.engine.Constants
import org.h2.engine.SysProperties
import org.h2.store.DataHandler
import org.h2.store.FileStore
import org.h2.store.FileStoreInputStream
import org.h2.store.fs.FileUtils
import org.h2.value.ValueLob
import java.io.BufferedInputStream
import java.io.InputStream

/**
 * LOB data stored in a temporary file.
 */
class LobDataFile(var handler: DataHandler?,
                  /**
                   * If the LOB is a temporary LOB being managed by a temporary ResultSet, it
                   * is stored in a temporary file.
                   */
                  val fileName: String?,
                  val tempFile: FileStore?) : LobData() {


    override fun remove(value: ValueLob?) {
        if (fileName == null) return

        tempFile?.stopAutoDelete()
        // synchronize on the database, to avoid concurrent temp file
        // creation / deletion / backup
        synchronized(handler!!.getLobSyncObject()) { FileUtils.delete(fileName) }
    }

    override fun getInputStream(precision: Long): InputStream {
        val store = handler!!.openFile(fileName!!, "r", true)
        val alwaysClose = SysProperties.lobCloseBetweenReads
        return BufferedInputStream(
                FileStoreInputStream(store, false, alwaysClose),
                Constants.IO_BUFFER_SIZE)
    }
}