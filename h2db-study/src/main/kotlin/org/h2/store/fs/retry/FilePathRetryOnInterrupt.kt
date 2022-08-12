package org.h2.store.fs.retry

import org.h2.store.fs.FilePathWrapper
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * A file system that re-opens and re-tries the operation if the file was
 * closed, because a thread was interrupted. This will clear the interrupt flag.
 * It is mainly useful for applications that call Thread. Interrupt by mistake.
 */
open class FilePathRetryOnInterrupt : FilePathWrapper() {

    override val scheme: String = "retry"

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = FileRetryOnInterrupt(name.substring(scheme.length + 1), mode)
}