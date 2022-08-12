package org.h2.store.fs.niomapped

import org.h2.store.fs.FilePathWrapper
import java.io.IOException
import java.nio.channels.FileChannel

/**
 * This file system stores files on disk and uses java.nio to access the files.
 * This class used memory mapped files.
 */
open class FilePathNioMapped() : FilePathWrapper() {

    override val scheme: String = "nioMapped"

    @Throws(IOException::class)
    override fun open(mode: String): FileChannel = FileNioMapped(filename = name.substring(scheme.length + 1), mode)
}