package org.h2.message

import org.h2.api.ErrorCode
import org.h2.engine.Constants
import org.h2.jdbc.JdbcException
import org.h2.message.DbException.Companion.get
import org.h2.store.fs.FileUtils
import org.h2.util.IOUtils
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter
import java.io.Writer
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicReferenceArray
import kotlin.math.max

/**
 * The trace mechanism is the logging facility of this database. There is
 * usually one trace system per database. It is called 'trace' because the term
 * 'log' is already used in the database domain and means 'transaction log'. It
 * is possible to write after close was called, but that means for each write
 * the file will be opened and closed again (which is slower)
 */
open class TraceSystem(private var fileName: String?) : TraceWriter {
    companion object {
        /**
         * The parent trace level should be used.
         */
        const val PARENT: Int = -1

        /**
         * This trace level means nothing should be written.
         */
        const val OFF: Int = 0

        /**
         * This trace level means only errors should be written.
         */
        const val ERROR: Int = 1

        /**
         * This trace level means errors and informatonal messages should be
         * written.
         */
        const val INFO: Int = 2

        /**
         * This trace level means all type of messages should be written.
         */
        const val DEBUG: Int = 3

        /**
         * This trace level means all type of messages should be written, but
         * instead of using the trace file the messages should be written to SLF4J
         */
        const val ADAPTER: Int = 4

        /**
         * The default level of system out trace messages.
         */
        const val DEFAULT_TRACE_LEVEL_SYSTEM_OUT: Int = OFF

        /**
         * The default level for file trace messages.
         */
        const val DEFAULT_TRACE_LEVEL_FILE: Int = ERROR

        /**
         * The default maximum trace file size. It is currently 64MB. Additionally,
         * there could be a .old file of the same size.
         */
        const val DEFAULT_MAX_FILE_SIZE: Int = 64 * 1024 * 1024

        const val CHECK_SIZE_EACH_WRITES: Int = 4096
    }

    private var levelSystemOut: Int = DEFAULT_TRACE_LEVEL_SYSTEM_OUT
    private var levelFile: Int = DEFAULT_TRACE_LEVEL_FILE
    private var levelMax: Int = 0
    private var maxFileSize: Int = DEFAULT_MAX_FILE_SIZE
    private val traces: AtomicReferenceArray<Trace> = AtomicReferenceArray(Trace.MODULE_NAMES.size)
    private val dateFormat: SimpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd HH:mm:ss ") }
    private var fileWriter: Writer? = null
    private var printWriter: PrintWriter? = null

    /**
     * Starts at -1 so that we check the file size immediately upon open. This
     * can be important if we open and close the trace file without managing to
     * have written CHECK_SIZE_EACH_WRITES bytes each time.
     */
    private var checkSize: Int = -1
    private var closed: Boolean = false
    private var writingErrorLogged: Boolean = false
    private var writer: TraceWriter = this
    var sysOut: PrintStream = System.out

    init {
        updateLevel()
    }

    private fun updateLevel(): Unit {
        levelMax = max(levelSystemOut, levelFile)
    }

    /**
     * Set the file trace level.
     *
     * @param level the new level
     */
    open fun setLevelFile(level: Int) {
        if (level == ADAPTER) {
            val adapterClass = "org.h2.message.TraceWriterAdapter"
            try {
                writer = Class.forName(adapterClass).getDeclaredConstructor().newInstance() as TraceWriter
            } catch (e: Throwable) {
                write(ERROR, Trace.DATABASE, adapterClass, get(ErrorCode.CLASS_NOT_FOUND_1, e, adapterClass))
                return
            }
            var name = fileName
            if (name != null) {
                if (name.endsWith(Constants.SUFFIX_TRACE_FILE)) {
                    name = name.substring(0, name.length - Constants.SUFFIX_TRACE_FILE.length)
                }
                val idx = max(name.lastIndexOf('/'), name.lastIndexOf('\\'))
                if (idx >= 0) {
                    name = name.substring(idx + 1)
                }
                writer.setName(name)
            }
        }
        levelFile = level
        updateLevel()
    }

    /**
     * Get or create a trace object for this module id. Trace modules with id
     * are cached.
     * @param moduleId module id
     * @return the trace object
     */
    fun getTrace(moduleId: Int): Trace {
        var t: Trace? = traces[moduleId]
        if (t == null) {
            t = Trace(writer, moduleId)
            if (!traces.compareAndSet(moduleId, null, t)) {
                t = traces[moduleId]
            }
        }
        return t!!
    }

    /**
     * Create a trace object for this module. Trace modules with names are not
     * cached.
     *
     * @param module the module name
     * @return the trace object
     */
    open fun getTrace(module: String): Trace = Trace(writer, module)

    @Synchronized
    private fun closeWriter() {
        if (printWriter != null) {
            printWriter!!.flush()
            printWriter!!.close()
            printWriter = null
        }
        if (fileWriter != null) {
            try {
                fileWriter!!.close()
            } catch (e: IOException) {
                // ignore
            }
            fileWriter = null
        }
    }

    /**
     * Close the writers, and the files if required. It is still possible to
     * write after closing, however after each write the file is closed again
     * (slowing down tracing).
     */
    open fun close() {
        closeWriter()
        closed = true
    }

    override fun setName(name: String) {}

    @Synchronized
    private fun format(module: String, s: String): String {
        return dateFormat.format(System.currentTimeMillis()) + module + ": " + s
    }

    override fun write(level: Int, module: String, s: String, t: Throwable?) {
        if (level <= levelSystemOut || level > levelMax) {
            // level <= levelSystemOut: the system out level is set higher
            // level > this.level: the level for this module is set higher
            sysOut.println(format(module, s))
            if (t != null && levelSystemOut == DEBUG) {
                t.printStackTrace(sysOut)
            }
        }
        if (fileName != null) {
            if (level <= levelFile) {
                writeFile(format(module, s), t)
            }
        }
    }

    @Synchronized
    private fun writeFile(s: String, t: Throwable?) {
        try {
            checkSize = (checkSize + 1) % CHECK_SIZE_EACH_WRITES
            if (checkSize == 0) {
                closeWriter()
                if (maxFileSize > 0 && FileUtils.size(fileName) > maxFileSize) {
                    val old = "$fileName.old"
                    FileUtils.delete(old)
                    FileUtils.move(fileName!!, old)
                }
            }
            if (!openWriter()) return

            printWriter!!.println(s)
            if (t != null) {
                if (levelFile == ERROR && t is JdbcException) {
                    val code = t.getErrorCode()
                    if (ErrorCode.isCommon(code)) {
                        printWriter!!.println(t)
                    } else {
                        t.printStackTrace(printWriter)
                    }
                } else {
                    t.printStackTrace(printWriter)
                }
            }

            printWriter!!.flush()
            if (closed) closeWriter()
        } catch (e: Exception) {
            logWritingError(e)
        }
    }

    private fun logWritingError(e: java.lang.Exception) {
        if (writingErrorLogged) return

        writingErrorLogged = true
        val se: Exception = get(ErrorCode.TRACE_FILE_ERROR_2, e, fileName, e.toString())
        // print this error only once
        fileName = null
        sysOut.println(se)
        se.printStackTrace()
    }

    private fun openWriter(): Boolean {
        if (printWriter != null) return true
        try {
            FileUtils.createDirectories(FileUtils.getParent(fileName!!))
            if (FileUtils.exists(fileName!!) && !FileUtils.canWrite(fileName!!)) {
                // read only database: don't log error if the trace file
                // can't be opened
                return false
            }
            fileWriter = IOUtils.getBufferedWriter(FileUtils.newOutputStream(fileName!!, true))
            printWriter = PrintWriter(fileWriter, true)
            return true
        } catch (e: java.lang.Exception) {
            logWritingError(e)
            return false
        }
    }

    override fun write(level: Int, moduleId: Int, s: String, t: Throwable?) {
        write(level, Trace.MODULE_NAMES[moduleId], s, t)
    }

    override fun isEnabled(level: Int): Boolean {
        return if (levelMax == ADAPTER) writer.isEnabled(level) else level <= levelMax
    }

    /**
     * Set the trace level to use for System.out
     *
     * @param level the new level
     */
    open fun setLevelSystemOut(level: Int) {
        levelSystemOut = level
        updateLevel()
    }
}