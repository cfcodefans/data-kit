package org.h2.store.fs.split

import org.h2.message.DbException
import org.h2.mvstore.DataUtils
import org.h2.store.fs.FileBaseDefault
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import kotlin.math.min

/**
 * A file that may be split into multiple smaller files.
 */
open class FileSplit(private val filePath: FilePathSplit,
                     private val mode: String,
                     private val maxLength: Long,
                     private var list: Array<FileChannel>,
                     @Volatile private var length: Long) : FileBaseDefault() {


    @Synchronized
    @Throws(IOException::class)
    override fun implCloseChannel() {
        for (c in list) {
            c.close()
        }
    }

    override fun size(): Long = length

    @Synchronized
    @Throws(IOException::class)
    override fun read(dst: ByteBuffer, position: Long): Int {
        var len = dst.remaining()
        if (len == 0) return 0

        len = min(len.toLong(), length - position).toInt()

        if (len <= 0) return -1

        val offset = position % maxLength
        len = Math.min(len.toLong(), maxLength - offset).toInt()

        return getFileChannel(position).read(dst, offset)
    }

    @Throws(IOException::class)
    private fun getFileChannel(position: Long): FileChannel {
        val id = (position / maxLength).toInt()
        while (id >= list.size) {
            list += filePath.getBase(list.size).open(mode)
        }
        return list[id]
    }

    @Synchronized
    @Throws(IOException::class)
    override fun force(metaData: Boolean) {
        for (c in list) {
            c.force(metaData)
        }
    }

    @Throws(IOException::class)
    override fun implTruncate(newLength: Long) {
        if (newLength >= length) return

        val newFileCount = 1 + (newLength / maxLength).toInt()

        if (newFileCount < list.size) {
            // delete some of the files
//            val newList = arrayOfNulls<FileChannel>(newFileCount)
            // delete backwards, so that truncating is somewhat transactional
            for (i in list.size - 1 downTo newFileCount) {
                // verify the file is writable
                list[i].truncate(0)
                list[i].close()
                try {
                    filePath.getBase(i).delete()
                } catch (e: DbException) {
                    throw DataUtils.convertToIOException(e)
                }
            }
//            System.arraycopy(list, 0, newList, 0, newList.size)
            list = list.sliceArray(0..newFileCount)
        }
        val size = newLength - maxLength * (newFileCount - 1)
        list[list.size - 1].truncate(size)
        length = newLength
    }

    @Synchronized
    @Throws(IOException::class)
    override fun write(src: ByteBuffer, position: Long): Int {
        var position = position

        if (position >= length && position > maxLength) {
            // may need to extend and create files
            val oldFilePointer = position
            var x = length - length % maxLength + maxLength
            while (x < position) {
                if (x > length) {
                    // expand the file size
                    position(x - 1)
                    write(ByteBuffer.wrap(ByteArray(1)))
                }
                position = oldFilePointer
                x += maxLength
            }
        }
        val offset = position % maxLength
        val len = src.remaining()
        val channel = getFileChannel(position)
        var l = Math.min(len.toLong(), maxLength - offset).toInt()
        if (l == len) {
            l = channel.write(src, offset)
        } else {
            val oldLimit = src.limit()
            src.limit(src.position() + l)
            l = channel.write(src, offset)
            src.limit(oldLimit)
        }
        length = Math.max(length, position + l)
        return l
    }

    @Synchronized
    @Throws(IOException::class)
    override fun tryLock(position: Long, size: Long, shared: Boolean): FileLock = list[0].tryLock(position, size, shared)

    override fun toString(): String = filePath.toString()
}