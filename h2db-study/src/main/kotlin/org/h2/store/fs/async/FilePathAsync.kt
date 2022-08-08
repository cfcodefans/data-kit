package org.h2.store.fs.async

import org.h2.store.fs.FilePathWrapper
import java.nio.channels.FileChannel

/**
 * This file system stores files on disk and uses
 * java.nio.channels.AsynchronousFileChannel to access the files.
 */
open class FilePathAsync() : FilePathWrapper() {
    override val scheme: String = "async"

    override fun open(mode: String): FileChannel = FileAsync(name.substring(scheme.length + 1), mode)
}