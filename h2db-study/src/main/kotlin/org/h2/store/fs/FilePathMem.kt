package org.h2.store.fs

import org.h2.compress.CompressLZF
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

/**
 * This class contains the data of an in-memory random access file.
 * Data compression using the LZF algorithm is supported as well.
 */
class FileMemData(val name: String, val compress: Boolean) {

    val id: Int = name.hashCode()
    var data: Array<AtomicReference<ByteArray>> = emptyArray()
    var lastModified: Long = System.currentTimeMillis()
    var isReadOnly: Boolean = false
    var isLockedExclusive: Boolean = false
    var sharedLockCount: Int = 0

    /**
     * Get the page if it exists.
     *
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
     *
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
        if (isLockedExclusive) isLockedExclusive = false
        else if (sharedLockCount > 0) sharedLockCount--
        else throw IOException("not locked")
    }


    companion object {
        const val CACHE_SIZE: Int = 8
        const val BLOCK_SIZE_SHIFT: Int = 10
        const val BLOCK_SIZE: Int = 1 shl BLOCK_SIZE_SHIFT
        const val BLOCK_SIZE_MASK: Int = BLOCK_SIZE - 1
        val LZF: CompressLZF = CompressLZF()
        val BUFFER: ByteArray = ByteArray(BLOCK_SIZE * 2)
        val COMPRESSED_EMPTY_BLOCK: ByteArray = run {
            val len: Int = LZF.compress(ByteArray(BLOCK_SIZE), BLOCK_SIZE, BUFFER, 0)
            Arrays.copyOf(BUFFER, len)
        }

        /**
         * Points to a block of bytes that needs to be compressed
         */
        class CompressItem(val file: FileMemData, val page: Int) {
            override fun hashCode(): Int = page xor file.getId()

            override fun equals(o: Any?): Boolean {
                if (o is CompressItem) {
                    return o.page == page && o.file === file
                }
                return false
            }
        }


        class Cache<K, V>(val _size: Int) : LinkedHashMap<K, V>(_size) {
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
        val DIRECTORY: FileMemData = FileMemData()
    }
}