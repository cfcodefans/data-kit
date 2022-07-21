package org.h2.store.fs.mem

import org.h2.store.fs.FileBaseDefault
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonWritableChannelException

/**
 * This class represents an in-memory file.
 */
open class FileMem(val data: FileMemData, private val readOnly: Boolean) : FileBaseDefault() {

    @Volatile
    private var closed = false

    override fun size(): Long = data.length()

    @Throws(IOException::class)
    override fun implTruncate(newLength: Long) {
        // compatibility with JDK FileChannel#truncate
        if (readOnly) throw NonWritableChannelException()
        if (closed) throw ClosedChannelException()

        if (newLength < size()) {
            data.touch(false)
            data.truncate(newLength)
        }
    }
}