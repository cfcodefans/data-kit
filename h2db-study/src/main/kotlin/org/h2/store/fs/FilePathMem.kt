package org.h2.store.fs

import org.h2.api.ErrorCode
import org.h2.compress.CompressLZF
import org.h2.message.DbException
import org.h2.util.MathUtils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * This class contains the data of an in-memory random access file.
 * Data compression using the LZF algorithm is supported as well.
 */
class FileMemData(val name: String, val compress: Boolean) {

    val id: Int = name.hashCode()
    var length: Long = 0
    var data: Array<AtomicReference<ByteArray>> = emptyArray()
    var lastModified: Long = System.currentTimeMillis()
    var isReadOnly: Boolean = false
    var isLockedExclusive: Boolean = false
    var sharedLockCount: Int = 0

    /**
     * Get the page if it exists.
     * @param page the page id
     * @return the byte array, or null
     */
    fun getPage(page: Int): ByteArray? {
        val b = data
        return if (page >= b.size) null else b[page].get()
    }

    /**
     * Set the page data.
     * @param page the page id
     * @param oldData the old data
     * @param newData the new data
     * @param force whether the data should be overwritten even if the old data doesn't match
     */
    fun setPage(page: Int, oldData: ByteArray, newData: ByteArray, force: Boolean) {
        val b = data
        if (page >= b.size) return
        if (force) b[page].set(newData) else b[page].compareAndSet(oldData, newData)
    }

    /**
     * Lock the file in exclusive mode if possible
     * @return if locking was successful
     */
    @Synchronized
    fun lockExclusive(): Boolean {
        if (sharedLockCount > 0 || isLockedExclusive) return false
        isLockedExclusive = true
        return true
    }

    /**
     * Lock the file in shared mode if possible.
     * @return if locking was successful
     */
    @Synchronized
    fun lockShared(): Boolean {
        if (isLockedExclusive) return false
        sharedLockCount++
        return true
    }

    /**
     * Unlock the file.
     */
    @Synchronized
    @Throws(IOException::class)
    fun unlock() {
        when {
            isLockedExclusive -> isLockedExclusive = false
            sharedLockCount > 0 -> sharedLockCount--
            else -> throw IOException("not locked")
        }
    }

    private fun compressLater(page: Int): Unit {
        val c: CompressItem = CompressItem(this, page)
        synchronized(LZF) {
            COMPRESS_LATER.put(c, c)
        }
    }

    private fun expand(page: Int): ByteArray {
        val d: ByteArray = getPage(page)!!
        if (d.size == BLOCK_SIZE) return d
        val out: ByteArray = ByteArray(BLOCK_SIZE)
        if (d != COMPRESSED_EMPTY_BLOCK) {
            synchronized(LZF) {
                LZF.expand(d, 0, d.size, out, 0, BLOCK_SIZE)
            }
        }
        setPage(page, d, out, false)
        return out
    }

    /**
     * Compress the data in a byte array.
     * @param page which page to compress
     */
    fun compress(page: Int): Unit {
        val old: ByteArray? = getPage(page)
        if (old == null || old.size != BLOCK_SIZE) {
            // not yet initialized or already compressed
            return
        }
        synchronized(LZF) {
            val len: Int = LZF.compress(old, BLOCK_SIZE, BUFFER, 0)
            if (len <= BLOCK_SIZE) {
                //maybe data was changed in the meantime
                setPage(page, old, BUFFER.copyOf(len), false)
            }
        }
    }

    /**
     * Update the last modified time
     * @param openReadOnly if the file was opened in read-only mode
     */
    @Throws(IOException::class)
    fun touch(openReadOnly: Boolean): Unit {
        if (isReadOnly || openReadOnly) throw IOException("Read only")
        lastModified = System.currentTimeMillis()
    }

    private fun changeLength(len: Long): Unit {
        this.length = len
        val _len: Long = MathUtils.roundUpLong(len, BLOCK_SIZE.toLong())
        val blocks: Int = (_len shr BLOCK_SIZE_SHIFT).toInt()
        if (blocks == data.size) return
        val n: Array<AtomicReference<ByteArray>?> = data.copyOf(blocks)
        for (i in data.size until blocks) n[i] = AtomicReference(COMPRESSED_EMPTY_BLOCK)
        data = n.cast()
    }

    /**
     * Truncate the file.
     * @param newLength the new length
     */
    fun truncate(newLength: Long): Unit {
        changeLength(newLength)
        val end: Long = MathUtils.roundUpLong(newLength, BLOCK_SIZE.toLong())
        if (end == newLength) return
        val lastPage: Int = (newLength shr BLOCK_SIZE_SHIFT).toInt()
        val d:ByteArray = expand(lastPage)

    }

    companion object {
        const val CACHE_SIZE: Int = 8
        private const val BLOCK_SIZE_SHIFT: Int = 10
        private const val BLOCK_SIZE: Int = 1 shl BLOCK_SIZE_SHIFT
        const val BLOCK_SIZE_MASK: Int = BLOCK_SIZE - 1
        private val LZF: CompressLZF = CompressLZF()
        private val BUFFER: ByteArray = ByteArray(BLOCK_SIZE * 2)
        val COMPRESSED_EMPTY_BLOCK: ByteArray = run {
            val len: Int = LZF.compress(ByteArray(BLOCK_SIZE), BLOCK_SIZE, BUFFER, 0)
            Arrays.copyOf(BUFFER, len)
        }
        private val COMPRESS_LATER: Cache<CompressItem, CompressItem> = Cache(CACHE_SIZE)

        /**
         * Points to a block of bytes that needs to be compressed
         */
        class CompressItem(val file: FileMemData, val page: Int) {
            override fun hashCode(): Int {
                return page xor file.id
            }

            override fun equals(o: Any?): Boolean {
                return (o is CompressItem) && (o.page == page && o.file === file)
            }
        }

        class Cache<K, V>(private val _size: Int) : LinkedHashMap<K, V>(_size) {
            override fun put(key: K, value: V): V? = super.put(key, value)
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
                if (size < _size) return false
                val c: CompressItem = eldest.key as CompressItem
                c.file.compress(c.page)
                return true
            }
        }

    }
}

open class FilePathMem : FilePath {
    companion object {
        val MEMORY_FILES: TreeMap<String, FileMemData> = TreeMap()
        val DIRECTORY: FileMemData = FileMemData("", false)

        /**
         * Get the canonical path for this file name.
         * @param fileName the file name
         * @return the canonical path
         */
        fun getCanonicalPath(_fileName: String): String {
            var fileName: String = _fileName.replace('\\', '/')
            val idx: Int = fileName.indexOf(':') + 1
            if (fileName.length > idx && fileName[idx] != '/') {
                fileName = fileName.substring(0, idx) + "/" + fileName.substring(idx)
            }
            return fileName
        }
    }

    override val scheme: String = "memFS"

    override fun getPath(path: String): FilePathMem {
        val p: FilePathMem = FilePathMem()
        p.name = getCanonicalPath(path)
        return p
    }

    /**
     * Whether the file should be compressed.
     *
     * @return if it should be compressed.
     */
    open fun compressed(): Boolean = false

    private fun getMemoryFile(): FileMemData? {
        synchronized(MEMORY_FILES) {
            var m: FileMemData? = MEMORY_FILES[name]
            return when (m) {
                DIRECTORY -> throw DbException.get(ErrorCode.FILE_CREATION_FAILED_1, "$name (a directory with this name already exists)")
                null -> {
                    m = FileMemData(name, compressed())
                    MEMORY_FILES.put(name, m)
                    m
                }
                else -> m
            }
        }
    }

    override fun size(): Long = getMemoryFile()?.length ?: -1
}