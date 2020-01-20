package org.h2.engine

import org.h2.util.MathUtils
import org.h2.util.Utils
import org.h2.util.Utils.getProperty
import org.jetbrains.kotlin.konan.file.File
import kotlin.math.max

/**
 * The constants defined in this class are initialized from system properties.
 * Some system properties are per machine settings, and others are as a last
 * resort and temporary solution to work around a problem in the application or
 * database engine. Also there are system properties to enable features that
 * are not yet fully tested or that are not backward compatible.
 * <p>System properties can be set when starting the virtual machine:</p>
 *
 * <pre>
 *     java -Dh2.baseDir=/temp
 *     </pre>
 * They can be set within the application, but this must be done before loading
 * any classes of the database (before loading the JDBC driver):
 * <pre>
 *     System.setProperty(&quot;h2.baseDir&quot;, &quot;/temp&quot;);
 *     </pre>
 */
object SysProperties {
    /**
     * System property <code>h2.objectCacheSize</code> (default: 1024). <br/>
     * The maximum number of objects in the cache.
     * This value must be a power of 2.
     */
    val OBJECT_CACHE_SIZE: Int = try {
        MathUtils.nextPowerOf2(Utils.getProperty("h2.objectCacheSize", 1024))
    } catch (e: IllegalArgumentException) {
        throw IllegalStateException("Invalid h2.objectCacheSize", e)
    }

    /**
     * INTERNAL
     */
    val H2_SCRIPT_DIRECTORY: String = "h2.scriptDirectory"

    /**
     * System property <code>h2.scriptDirectory</code> (default: empty
     * string).<br/>
     * Relative or absolute directory where the script files are stored to
     * read from.
     * @return the current value
     */
    @JvmStatic
    fun getScriptDirectory(): String = Utils.getProperty(H2_SCRIPT_DIRECTORY, "")!!

    /**
     * INTERNAL
     */
    val H2_BROWSER: String = "h2.browser"

    /**
     * System property <code>file.separator</code>.<br/>
     * It is set by the system, and used to build absolute file names.
     */
    val FILE_SEPARATOR: String = File.separator

    /**
     * System property <code>line.separator</code>.<br/>
     * It is set by the system, and used by the script and trace tools.
     */
    val LINE_SEPARATOR: String = System.lineSeparator()

    /**
     * System property <code>user.home</code> (empty string if not set).<br/>
     * It is usually set by the system, and used as a replacement for ~ in file
     * names.
     */
    val USER_HOME: String = Utils.getProperty("user.home", "")!!

    /**
     * System property {@code h2.preview} (default: false).
     * <p>
     *     Controls default values of other properties. if {@code true} default
     *     values of other properties are changed to planned defaults for the 1.5.x
     *     versions of H2. Some other functionality may be also enabled or disabled.
     * </p>
     */
    val PREVIEW: Boolean = getProperty("h2.preview", false)

    /**
     * System property <code>h2.allowedClasses</code> (default: *).<br/>
     * Comma separated list of class names or prefixes.
     */
    val ALLOWED_CLASSES: String = getProperty("h2.allowedClasses", "")!!

    /**
     * System property <code>h2.enableAnonymousTLS</code> (default: true).<br/>
     * When using TLS connection, the anonymous cipher suites should be enabled.
     */
    val ENABLE_ANONYMOUS_TLS: Boolean = getProperty("h2.enableAnonymousTLS", true)

    /**
     * System property <code>h2.bindAddress</code> (default: null). <br/>
     * The bind address to use.
     */
    val BIND_ADDRESS: String? = getProperty("h2.bindAddress", null)

    /**
     * System property <code>h2.check</code>
     * (default: true for JDK/JRE, false for Android).<br/>
     * Optional additional checks in the database engine.
     */
    val CHECK: Boolean = getProperty("h2.check", "0.9" != (getProperty("java.sepecification.version", null)))

    /**
     * System property <code>h2.clientTraceDirectory</code> (default:
     * trace.db/).<br/>
     * Directory where the trace files of the JDBC client are sorted (only for client/server).
     */
    val CLIENT_TRACE_DIRECTORY: String = getProperty("h2.clientTraceDirectory", "trace.db/")!!

    /**
     * System property <code>h2.collatorCacheSize</code> (default: 32000). <br/>
     * The cache size for collation keys (in elements). Used when a collator has been
     * set for the database.
     */
    val COLLATOR_CACHE_SIZE: Int = getProperty("h2.collatorCacheSize", 32_000)

    /**
     * System property <code>h2.consoleTableColumns</code>
     * (default: 500).<br/>
     * Up to this many tables, the column names are listed.
     */
    val CONSOLE_MAX_TABLES_LIST_COLUMNS: Int = getProperty("h2.consoleTableColumns", 500)

    /**
     * System property <code>h2.consoleProcedureColumns</code>
     * (default: 300). <br/>
     * Up to this many procedures, the column names are list
     */
    val CONSOLE_MAX_PROCEDURES_LIST_COLUMNS: Int = getProperty("h2.consoleProcedureColumns", 300)

    /**
     * System property <code>h2.consoleSteram</code> (default: true). <br/>
     * H2 Console: stream query results.
     */
    val CONSOLE_STREAM: Boolean = getProperty("h2.consoleStream", true)

    /**
     * System property <code>h2.consoleTimeout</code> (default: 1800_000). <br/>
     * H2 console: session timeout in milliseconds. The default is 30 minutes.
     */
    val CONSOLE_TIMEOUT: Int = getProperty("h2.consoleTimeout", 30 * 60 * 1000)

    /**
     * System property <code>h2.dataSourceTraceLevel</code> (default: 1).<br/>
     * The trace level of the data source implementation. Default is 1 for error.
     */
    val DATASOURCE_TRACE_LEVEL: Int = getProperty("h2.dataSourceTraceLevel", 1)

    /**
     * System property <code>h2.delayWrongPasswordMin</code>
     * (default: 250).<br/>
     * The minimum delay in milliseconds before an exception is thrown for using
     * the wrong user name or password. This slows down brute force attacks. The
     * delay is reset to this value after a successful login. Unsuccessful
     * logins will double the time until DELAY_WRONG_PASSWORD_MAX.
     * To disable the delay, set the system property to 0.
     */
    val DELAY_WRONG_PASSWORD_MIN: Int = getProperty("h2.delayWrongPasswordMin", 250)

    /**
     * System property <code>h2.delayWrongPasswordMax</code>
     * (default: 4000). <br/>
     * The maximum delay in milliseconds before an exception is thrown for using the
     * wrong user name or password. This slows down brute force attacks. The delay
     * is reset after a successful login. The value 0 means there is no maximum delay.
     */
    val DELAY_WRONG_PASSWORD_MAX: Int = getProperty("h2.delayWrongPasswordMax", 4000)

    /**
     * System property <code>h2.javaSystemCompiler</code> (default: true). <br/>
     * Whether to use the Java system compiler
     * (ToolProvider.getSystemJavaCompiler()) if it is available to compile user
     * defined functions. If disabled or if the system compiler is not available,
     * the com.sun.tools.javac.compiler is used if available, and
     * "javac" (as an external process) is used if not.
     */
    val JAVA_SYSTEM_COMPILER: Boolean = getProperty("h2.javaSystemCompiler", true)

    /**
     * System property <code>h2.lobCloseBetweenReads</code>
     * (default: false). <br/>
     * Close LOB files between read operations.
     */
    val lobCloseBetweenReads: Boolean = getProperty("h2.lobCloseBetweenReads", false)

    /**
     * System property <code>h2.lobFilesPerDirectory</code>
     * (default: 256). <br/>
     * Maximum number of LOB files per directory.
     */
    val LOB_FILES_PER_DIRECTORY: Int = getProperty("h2.lobFilesPerDirectory", 256)

    /**
     * System property <code>h2.lobClientMaxSizeMemory</code> (default:
     * 1048576). <br/>
     * The maximum size of a LOB object to keep in memory on the client side
     * when using the server mode.
     */
    val LOB_CLIENT_MAX_SIZE_MEMORY: Int = getProperty("h2.lobClientMaxSizeMemory", 1024 * 1024)

    /**
     * System property <code>h2.maxFileRetry</code> (default: 16).<br/>
     * Number of times to retry file delete and rename. In Windows, files can't
     * be deleted if they are open. Waiting a bit can help (sometimes the Windows
     * Explorer opens the files for a short time) may help. Sometimes,
     * running garbage collection may close files if the user forgot to call
     * Connection.close() or InputStream.close().
     */
    val MAX_FILE_RETRY: Int = max(1, getProperty("h2.maxFileRetry", 16))

    /**
     * System property <code>h2.maxReconnect</code> (default: 3).<br/>
     * The maximum number of tries to reconnect in a row.
     */
    val MAX_RECONNECT: Int = getProperty("h2.maxReconnect", 3)

    /**
     * System property <code>h2.maxMemeroryRows</code>
     * (default: 40_000 per GB of available RAM). <br/>
     * The default maximum number of rows to be kept in memory in a result set.
     */
    val MAX_MEMORY_ROWS: Int = getAutoScaledForMemoryProperty("h2.maxMemoryRows", 40_000)

    /**
     * This method attempts to auto_scale some of our properties to take
     * advantage of more powerful machines out of the box. We assume that our
     * default properties are set correctly for approx. 1G of memory, and scale them up
     * if we have more
     */
    fun getAutoScaledForMemoryProperty(key: String?, defaultValue: Int): Int {
        val s: String? = getProperty(key, null)
        if (s != null) {
            try {
                return Integer.decode(s)
            } catch (e: NumberFormatException) {

            }
        }
        return Utils.scaleForAvailableMemory(defaultValue)
    }


}
