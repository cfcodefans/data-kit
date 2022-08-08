package org.h2.store.fs.niomapped

import org.h2.engine.SysProperties
import org.h2.store.fs.FileBaseDefault
import org.h2.store.fs.FileUtils
import org.h2.util.MemoryUnmapper
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.NonWritableChannelException
import java.nio.file.Paths

/**
 * Uses memory mapped files.
 * The file size is limited to 2 GB.
 */

open class FileNioMapped : FileBaseDefault {
    companion object {
        private const val GC_TIMEOUT_MS = 10000

        @Throws(IOException::class)
        private fun checkFileSizeLimit(length: Long) {
            if (length > Int.MAX_VALUE) throw IOException("File over 2GB is not supported yet when using this file system")
        }
    }

    private val name: String
    private val mode: MapMode
    private var channel: FileChannel?
    private var mapped: MappedByteBuffer? = null
    private var fileLength: Long = 0

    @Throws(IOException::class)
    constructor(filename: String, mode: String) {
        this.mode = if ("r" == mode) MapMode.READ_ONLY else MapMode.READ_WRITE
        this.name = filename
        this.channel = FileChannel.open(Paths.get(filename), FileUtils.modeToOptions(mode), *FileUtils.NO_ATTRIBUTES)

    }

    @Throws(IOException::class)
    private fun unMap() = apply {
        if (mapped == null) return@apply

        //first write all data
        mapped!!.force()

        // need to dispose old direct buffer, see bug
        //  https://bugs.openjdk.java.net/browse/JDK-4724038
        if (SysProperties.NIO_CLEANER_HACK) {
            if (MemoryUnmapper.unmap(mapped!!)) {
                mapped = null
                return@apply
            }
        }
        val bufferWeakRef: WeakReference<MappedByteBuffer> = WeakReference(mapped)
        mapped = null
        val stopAt = System.nanoTime() + GC_TIMEOUT_MS * 1000000L
        while (bufferWeakRef.get() != null) {
            if (System.nanoTime() - stopAt > 0L) {
                throw IOException("Timeout ($GC_TIMEOUT_MS ms) reached while trying to GC mapped buffer")
            }
            System.gc()
            Thread.yield()
        }
    }

    /**
     * Re-map byte buffer into memory, called when file size has changed or file was created
     */
    private fun reMap() = apply {
        if (mapped != null) unMap()

        fileLength = channel!!.size()
        checkFileSizeLimit(fileLength)
        // maps new MappedByteBuffer; the old one is disposed during GC
        mapped = channel!!.map(mode, 0, fileLength)
        val limit = mapped!!.limit()
        val capacity = mapped!!.capacity()
        if (limit < fileLength || capacity < fileLength) {
            throw IOException("Unable to map: length=$limit capacity=$capacity length=$fileLength")
        }
        if (SysProperties.NIO_LOAD_MAPPED) mapped!!.load()
    }

    @Throws(IOException::class)
    override fun implTruncate(newLength: Long) {
        // compatibility with JDK FileChannel#truncate
        if (mode === MapMode.READ_ONLY) {
            throw NonWritableChannelException()
        }
        if (newLength < size()) {
            setFileLength(newLength)
        }
    }

    @Synchronized
    @Throws(IOException::class)
    open fun setFileLength(newLength: Long) {
        if (mode === MapMode.READ_ONLY) {
            throw NonWritableChannelException()
        }
        checkFileSizeLimit(newLength)
        unMap()

        var i = 0
        while (true) {
            try {
                val length = channel!!.size()
                if (length >= newLength) {
                    channel!!.truncate(newLength)
                } else {
                    channel!!.write(ByteBuffer.wrap(ByteArray(1)), newLength - 1)
                }
                break
            } catch (e: IOException) {
                if (i > 16 || !e.toString().contains("user-mapped section open")) {
                    throw e
                }
            }
            System.gc()
            i++
        }
        reMap()
    }

    override fun implCloseChannel() {
        if (channel == null) return
        unMap()
        channel!!.close()
        channel = null
    }

    override fun toString(): String = "nioMapped:$name"

    @Synchronized
    @Throws(IOException::class)
    override fun size(): Long = fileLength


}