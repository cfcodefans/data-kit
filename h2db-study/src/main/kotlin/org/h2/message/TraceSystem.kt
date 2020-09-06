package org.h2.message

import org.jetbrains.kotlin.utils.sure
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
open class TraceSystem(private var fileName: String) : TraceWriter, AutoCloseable {
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
    private lateinit var fileWriter: Writer
    private lateinit var printWriter: PrintWriter
    /**
     * Starts at -1 so that we check the file size immediately upon open. This
     * can be important if we open and close the trace file without managing to
     * have written CHECK_SIZE_EACH_WRITES bytes each time.
     */
    private var checkSize: Int = -1
    private var closed: Boolean = false
    private var writingErrorLogged: Boolean = false
    private val writer: TraceWriter = this
    var sysOut: PrintStream = System.out

    init {
        updateLevel()
    }

    private fun updateLevel(): Unit {
        levelMax = max(levelSystemOut, levelFile)
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

        }
    }
}