package org.h2.store

import org.h2.Driver
import org.h2.api.ErrorCode
import org.h2.message.DbException
import org.h2.message.Trace
import org.h2.message.TraceSystem
import org.h2.store.fs.FileUtils
import org.h2.util.MathUtils
import org.h2.util.SortedProperties
import org.h2.util.StringUtils
import java.io.IOException
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Paths
import java.util.Properties

/**
 * The file lock is used to lock a database so that only one process can write
 * to it. It uses a cooperative locking protocol. Usually a .lock.db file is
 * used, but locking by creating a socket is supported as well.
 */
open class FileLock(
    traceSystem: TraceSystem?,
    /**
     * The lock file name.
     */
    @Volatile
    private var fileName: String? = null,
    /**
     * The number of milliseconds to sleep after checking a file.
     */
    private val sleep: Int = 0) : Runnable {

    companion object {
        private const val MAGIC = "FileLock"
        private const val FILE = "file"
        private const val SOCKET = "socket"
        private const val RANDOM_BYTES = 16
        private const val SLEEP_GAP = 25
        private const val TIME_GRANULARITY = 2000

        private fun getExceptionFatal(reason: String, t: Throwable?): DbException = DbException.get(ErrorCode.ERROR_OPENING_DATABASE_1, t, reason)

        /**
         * Aggressively read last modified time, to work-around remote filesystems.
         *
         * @param fileName file name to check
         * @return last modified date/time in milliseconds UTC
         */
        private fun aggressiveLastModified(fileName: String): Long {
            /*
         * Some remote filesystem, e.g. SMB on Windows, can cache metadata for
         * 5-10 seconds. To work around that, do a one-byte read from the
         * underlying file, which has the effect of invalidating the metadata
         * cache.
         */
            kotlin.runCatching {
                FileChannel.open(Paths.get(fileName),
                    FileUtils.RWS,
                    *FileUtils.NO_ATTRIBUTES).use { f ->
                    f.read(ByteBuffer.wrap(ByteArray(1)))
                }
            }
            return FileUtils.lastModified(fileName)
        }

        private fun sleep(time: Int) = try {
            Thread.sleep(time.toLong())
        } catch (e: InterruptedException) {
            throw getExceptionFatal("Sleep interrupted", e)
        }
    }

    /**
     * The trace object.
     */
    private val trace: Trace? = traceSystem?.getTrace(Trace.FILE_LOCK)

    /**
     * Whether the file is locked.
     */
    @Volatile
    private var locked = false

    /**
     * The server socket (only used when using the SOCKET mode).
     */
    @Volatile
    private var serverSocket: ServerSocket? = null

    /**
     * The last time the lock file was written.
     */
    private var lastWrite: Long = 0

    private var method: String? = null
    private var properties: Properties? = null
    private var uniqueId: String? = null
    private var watchdog: Thread? = null

    /**
     * Save the lock file.
     * @return the saved properties
     */
    open fun save(): Properties? = try {
        FileUtils.newOutputStream(fileName!!, false).use { out -> properties!!.store(out, MAGIC) }
        lastWrite = aggressiveLastModified(fileName!!)
        if (trace!!.isDebugEnabled()) trace.debug("save $properties")
        properties
    } catch (e: IOException) {
        throw getExceptionFatal("Could not save properties $fileName", e)
    }

    /**
     * Load the properties file.
     *
     * @return the properties
     */
    open fun load(): Properties {
        var lastException: IOException? = null
        for (i in 0..4) {
            lastException = try {
                val p2: Properties = SortedProperties.loadProperties(fileName)
                if (trace!!.isDebugEnabled()) trace.debug("load $p2")
                return p2
            } catch (e: IOException) {
                e
            }
        }
        throw getExceptionFatal("Could not load properties $fileName", lastException!!)
    }

    private fun getExceptionAlreadyInUse(reason: String): DbException? {
        var e = DbException.get(ErrorCode.DATABASE_ALREADY_OPEN_1, reason)
        if (fileName == null) return e

        try {
            val prop: Properties = load()
            prop.getProperty("server")?.let { server ->
                e = e.addSQL("$server/${prop.getProperty("id")}")
            }
        } catch (e2: DbException) {
            // ignore
        }
        return e
    }

    /**
     * Aggressively read last modified time, to work-around remote filesystems.
     *
     * @param fileName file name to check
     * @return last modified date/time in milliseconds UTC
     */
    private fun aggressiveLastModified(fileName: String): Long {
        /*
         * Some remote filesystem, e.g. SMB on Windows, can cache metadata for
         * 5-10 seconds. To work around that, do a one-byte read from the
         * underlying file, which has the effect of invalidating the metadata
         * cache.
         */
        kotlin.runCatching {
            FileChannel.open(Paths.get(fileName), FileUtils.RWS, *FileUtils.NO_ATTRIBUTES).use { f ->
                val b = ByteBuffer.wrap(ByteArray(1))
                f.read(b)
            }
        }
        return FileUtils.lastModified(fileName)
    }

    /**
     * Get the file locking method type given a method name.
     *
     * @param method the method name
     * @return the method type
     * @throws DbException if the method name is unknown
     */
    open fun getFileLockMethod(method: String?): FileLockMethod =
        if (method == null || method.equals("FILE", ignoreCase = true)) FileLockMethod.FILE
        else if (method.equals("NO", ignoreCase = true)) FileLockMethod.NO
        else if (method.equals("SOCKET", ignoreCase = true)) FileLockMethod.SOCKET
        else if (method.equals("FS", ignoreCase = true)) FileLockMethod.FS
        else throw DbException.get(ErrorCode.UNSUPPORTED_LOCK_METHOD_1, method)

    override fun run() {
        try {
            while (locked && fileName != null) {
                // trace.debug("watchdog check");
                try {
                    if (FileUtils.exists(fileName!!).not()
                        || aggressiveLastModified(fileName!!) != lastWrite) {
                        save()
                    }
                    Thread.sleep(sleep.toLong())
                } catch (_: OutOfMemoryError) {
                } catch (_: NullPointerException) {
                } catch (_: InterruptedException) {
                } catch (e: Exception) {
                    trace!!.debug(e, "watchdog")
                }
            }
            while (true) {
                // take a copy, so we don't get an NPE between checking it and using it
                val local = serverSocket ?: break
                try {
                    trace!!.debug("watchdog accept")
                    local.accept().close()
                } catch (e: Exception) {
                    trace!!.debug(e, "watchdog")
                }
            }
        } catch (e: Exception) {
            trace!!.debug(e, "watchdog")
        }
        trace!!.debug("watchdog end")
    }

    private fun setUniqueId() {
        val bytes = MathUtils.secureRandomBytes(RANDOM_BYTES)
        val random = StringUtils.convertBytesToHex(bytes)
        uniqueId = java.lang.Long.toHexString(System.currentTimeMillis()) + random
        properties!!.setProperty("id", uniqueId)
    }

    private fun waitUntilOld() {
        for (i in 0 until (2 * TIME_GRANULARITY / SLEEP_GAP)) {
            val last: Long = FileLock.aggressiveLastModified(fileName!!)
            val dist = System.currentTimeMillis() - last
            if (dist < -TIME_GRANULARITY) {
                // lock file modified in the future -
                // wait for a bit longer than usual
                try {
                    Thread.sleep(2 * sleep.toLong())
                } catch (e: java.lang.Exception) {
                    trace!!.debug(e, "sleep")
                }
                return
            } else if (dist > TIME_GRANULARITY) {
                return
            }
            try {
                Thread.sleep(SLEEP_GAP.toLong())
            } catch (e: java.lang.Exception) {
                trace!!.debug(e, "sleep")
            }
        }
        throw getExceptionFatal("Lock file recently modified", null)
    }

    private fun lockFile() {
        method = FILE
        properties = SortedProperties()
        properties!!.setProperty("method", method.toString())
        setUniqueId()
        FileUtils.createDirectories(FileUtils.getParent(fileName!!))

        if (!FileUtils.createFile(fileName!!)) {
            waitUntilOld()
            val m2 = load().getProperty("method", FILE)
            if (m2 != FILE) {
                throw getExceptionFatal("Unsupported lock method $m2", null)
            }
            save()
            FileLock.sleep(2 * sleep)
            if (load() != properties) {
                throw getExceptionAlreadyInUse("Locked by another process: $fileName")!!
            }
            FileUtils.delete(fileName!!)
            if (!FileUtils.createFile(fileName!!)) {
                throw getExceptionFatal("Another process was faster", null)
            }
        }
        save()
        FileLock.sleep(SLEEP_GAP)
        if (load() != properties) {
            fileName = null
            throw getExceptionFatal("Concurrent update", null)
        }
        locked = true
        watchdog = Thread(this, "H2 File Lock Watchdog $fileName")
        Driver.setThreadContextClassLoader(watchdog!!)
        watchdog!!.isDaemon = true
        watchdog!!.priority = Thread.MAX_PRIORITY - 1
        watchdog!!.start()
    }
}