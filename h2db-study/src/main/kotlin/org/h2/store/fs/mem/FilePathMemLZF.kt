package org.h2.store.fs.mem

/**
 * A memory file system that compresses blocks to conserve memory.
 */
open class FilePathMemLZF : FilePathMem() {

    override fun getPath(path: String?): FilePathMem? = FilePathMemLZF().apply { name = getCanonicalPath(path) }

    override fun compressed(): Boolean = true

    override fun getScheme(): String = "memLZF"
}