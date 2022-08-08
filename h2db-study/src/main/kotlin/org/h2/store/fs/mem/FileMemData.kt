package org.h2.store.fs.mem

import org.h2.compress.CompressLZF
import org.h2.util.MathUtils
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.IOException
import java.nio.channels.NonWritableChannelException
import java.util.concurrent.atomic.AtomicReference

/**
 * This class contains the data of an in-memory random access file.
 * Data compression using the LZF algorithm is supported as well.
 */
open class FileMemData(var name: String, private val compress: Boolean) {

    val id: Int = name.hashCode()

    @Volatile
    private var length: Long = 0

    fun getLen() = length

    private var data: Array<AtomicReference<ByteArray>> = emptyArray()
    var lastModified: Long = 0

    private var isReadOnly: Boolean = false
    private var isLockedExclusive: Boolean = false
    private var sharedLockCount: Int = 0

    /**
     * Set the read-only flag.
     *
     * @return true
     */
    open fun setReadOnly(): Boolean {
        isReadOnly = true
        return true
    }

    /**
     * Check whether writing is allowed.
     *
     * @return true if it is
     */
    open fun canWrite(): Boolean = !isReadOnly


    companion object {
        /**
         * Points to a block of bytes that needs to be compressed.
         */
        internal class CompressItem(
            /**
             * The file.
             */
            var file: FileMemData,

            /**
             * The page to compress.
             */
            var page: Int = 0) {

            override fun hashCode(): Int = page xor file.id

            override fun equals(o: Any?): Boolean = (o is CompressItem) && (o.page == page && o.file === file)
        }

        /**
         * This small cache compresses the data if an element leaves the cache.
         */
        internal class Cache<K, V>(override val size: Int) : LinkedHashMap<K, V>(size, 0.75.toFloat(), true) {
            @Synchronized
            override fun put(key: K, value: V): V? = super.put(key, value)

            override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean {
                if (super.size < size) return false

                val c: CompressItem = eldest.key as CompressItem
                c.file.compress(c.page)
                return true
            }

            companion object {
                private const val serialVersionUID = 1L
            }
        }

        init {

        }

        private const val CACHE_SIZE = 8
        private const val BLOCK_SIZE_SHIFT = 10
        private const val BLOCK_SIZE = 1 shl BLOCK_SIZE_SHIFT
        private const val BLOCK_SIZE_MASK = BLOCK_SIZE - 1
        private val LZF = CompressLZF()
        private val BUFFER = ByteArray(BLOCK_SIZE * 2)
        private val COMPRESSED_EMPTY_BLOCK: ByteArray = let {
            val n = ByteArray(BLOCK_SIZE)
            val len = LZF.compress(n, 0, BLOCK_SIZE, BUFFER, 0)
            BUFFER.copyOf(len)
        }

        private val COMPRESS_LATER: Cache<CompressItem, CompressItem> = Cache<CompressItem, CompressItem>(CACHE_SIZE)
    }

    /**
     * Get the page if it exists.
     *
     * @param page the page id
     * @return the byte array, or null
     */
    private fun getPage(page: Int): ByteArray? = if (page >= data.size) null else data[page].get()

    /**
     * Set the page data.
     *
     * @param page the page id
     * @param oldData the old data
     * @param newData the new data
     * @param force whether the data should be overwritten even if the old data
     * doesn't match
     */
    private fun setPage(page: Int, oldData: ByteArray, newData: ByteArray, force: Boolean) {
        if (page >= data.size) return

        if (force) data[page].set(newData)
        else data[page].compareAndSet(oldData, newData)
    }

    /**
     * Lock the file in exclusive mode if possible.
     *
     * @return if locking was successful
     */
    @Synchronized
    open fun lockExclusive(): Boolean {
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
    open fun lockShared(): Boolean {
        if (isLockedExclusive) return false
        sharedLockCount++
        return true
    }

    /**
     * Unlock the file.
     */
    @Synchronized
    @Throws(IOException::class)
    open fun unlock() {
        if (isLockedExclusive) isLockedExclusive = false
        else if (sharedLockCount > 0) sharedLockCount--
        else throw IOException("not locked")
    }

    private fun compressLater(page: Int) {
        val c: CompressItem = CompressItem(file = this, page = page)
        synchronized(LZF) { COMPRESS_LATER.put(c, c) }
    }

    private fun expand(page: Int): ByteArray {
        val d = getPage(page)
        if (d!!.size == BLOCK_SIZE) return d

        val out = ByteArray(BLOCK_SIZE)
        if (d != COMPRESSED_EMPTY_BLOCK) {
            synchronized(LZF) { LZF.expand(d, 0, d!!.size, out, 0, BLOCK_SIZE) }
        }
        setPage(page, d!!, out, false)
        return out
    }

    /**
     * Compress the data in a byte array.
     *
     * @param page which page to compress
     */
    open fun compress(page: Int) {
        val old = getPage(page)
        if (old == null || old.size != BLOCK_SIZE) {
            // not yet initialized or already compressed
            return
        }
        synchronized(LZF) {
            val len = LZF.compress(old, 0, BLOCK_SIZE, BUFFER, 0)
            if (len <= BLOCK_SIZE) {
                val d = BUFFER.copyOf(len)
                // maybe data was changed in the meantime
                setPage(page, old, d, false)
            }
        }
    }

    /**
     * Update the last modified time.
     *
     * @param openReadOnly if the file was opened in read-only mode
     */
    open fun touch(openReadOnly: Boolean) {
        if (isReadOnly || openReadOnly) {
            throw NonWritableChannelException()
        }
        lastModified = System.currentTimeMillis()
    }

    private fun changeLength(len: Long) {
        length = len

        val blocks = (MathUtils.roundUpLong(len, BLOCK_SIZE.toLong()) ushr BLOCK_SIZE_SHIFT).toInt()
        if (blocks == data.size) return

        val n: Array<AtomicReference<ByteArray>> = data.copyOf(blocks).cast()
        for (i in data.size until blocks) {
            n[i] = AtomicReference(COMPRESSED_EMPTY_BLOCK)
        }
        data = n
    }

    /**
     * Truncate the file.
     *
     * @param newLength the new length
     */
    open fun truncate(newLength: Long) {
        changeLength(newLength)
        val end = MathUtils.roundUpLong(newLength, BLOCK_SIZE.toLong())
        if (end == newLength) return

        val lastPage = (newLength ushr BLOCK_SIZE_SHIFT).toInt()
        val d = expand(lastPage)
        val d2 = d.copyOf(d.size)
        for (i in (newLength and BLOCK_SIZE_MASK.toLong()).toInt() until BLOCK_SIZE) {
            d2[i] = 0
        }
        setPage(lastPage, d, d2, true)
        if (compress) {
            compressLater(lastPage)
        }
    }

    /**
     * Read or write.
     *
     * @param pos the position
     * @param b the byte array
     * @param off the offset within the byte array
     * @param len the number of bytes
     * @param write true for writing
     * @return the new position
     */
    open fun readWrite(pos: Long, b: ByteArray?, off: Int, len: Int, write: Boolean): Long {
        var pos = pos
        var off = off
        var len = len
        val end = pos + len
        if (end > length) {
            if (write) {
                changeLength(end)
            } else {
                len = (length - pos).toInt()
            }
        }
        while (len > 0) {
            val l = len.toLong().coerceAtMost(BLOCK_SIZE - (pos and BLOCK_SIZE_MASK.toLong())).toInt()
            val page = (pos ushr BLOCK_SIZE_SHIFT).toInt()
            val block = expand(page)
            val blockOffset = (pos and BLOCK_SIZE_MASK.toLong()).toInt()
            if (write) {
                val p2 = block.copyOf(block.size)
                System.arraycopy(b, off, p2, blockOffset, l)
                setPage(page, block, p2, true)
            } else {
                System.arraycopy(block, blockOffset, b, off, l)
            }
            if (compress) {
                compressLater(page)
            }
            off += l
            pos += l.toLong()
            len -= l
        }
        return pos
    }
}