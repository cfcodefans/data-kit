package org.h2.store.fs

import org.h2.store.fs.FilePath.Companion.get

object FileUtils {
    @JvmStatic
    fun exists(fileName: String): Boolean = get(fileName).exists()
}