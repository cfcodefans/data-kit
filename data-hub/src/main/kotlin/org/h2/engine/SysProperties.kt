package org.h2.engine

import org.h2.util.MathUtils
import org.h2.util.Utils
import org.h2.util.Utils.getProperty
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.util.suffixIfNot
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
     * System property <code>h2.maxTraceDataLength</code>
     * (default: 65535). <br/>
     * The maximum size of a LOB value that is written as data to the trace system.
     */
    val MAX_TRACE_DATA_LENGTH: Long = getProperty("h2.maxTraceDataLength", 65535).toLong()

    /**
     * System property <code>h2.modifyOnWrite</code> (default: false). <br/>
     * Only modify the database file when recovery is necessary, or when writing
     * to the database. If disabled, opening the database always writes to the file
     * (except if the database is read-only). When enabled, the serialized file
     * lock is faster.
     */
    val MODIFY_ON_WRITE: Boolean = getProperty("h2.modifyOnWrite", false)

    /**
     * System property <code>h2.nioClearnerHack</code> (default: false). <br/>
     * If enabled, use the reflection heck to un-map the mapped file if possible.
     * If disabled, System.gc() is called in a loop until the object is garbage collected.
     * See also
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4724038
     */
    val NIO_CLEANER_HACK: Boolean = getProperty("h2.nioCleanerHack", false)

    /**
     * System property <code>h2.objectCache</code> (default: true).<br/>
     * Cache commonly used values (numbers, strings). This is a shared cache
     * for all values.
     */
    val OBJECT_CACHE: Boolean = getProperty("h2.objectCache", true)

    /**
     * System property <code>h2.objectCacheMaxPerElementSize</code> (default: 4096). <br/>
     * The maximum size (precision) of an object in the cache.
     */
    val OBJECT_CACHE_MAX_PER_ELEMENT_SIZE: Int = getProperty("h2.objectCacheMaxPerElementSize", 4096)

    /**
     * System property <code>h2.objectCacheSize</code> (default: 1024).<br/>
     * The maximum number of objects in the cache.
     * This value must be a power of 2.
     */
    val OBJECT_CACHE_SIZE: Int = try {
        MathUtils.nextPowerOf2(getProperty("h2.objectCacheSize", 1024))
    } catch (e: java.lang.IllegalArgumentException) {
        throw java.lang.IllegalArgumentException("Invalid h2.objectCacheSize", e)
    }

    /**
     * System property {@code h2.oldResultSetGetObject}, {@code true} by default
     * unless {@code h2.preview} is enabled.
     * <p>
     * If {@code true} return {@code Byte} and {@code Short} from
     * {@code ResultSet#getObject(int)} and {@code ResultSet#getObject(String)}
     * for {@code TINYINT} and {@code SMALLINT} values.
     * </p>
     * <p>
     * If {@code false} return {@code Integer} for them as specified in JDBC
     * specification (see Mapping from JDBC Types to Java Object Types).
     * </p>
     */
    val OLD_RESULT_SET_GET_OBJECT: Boolean = getProperty("h2.oldResultSetGetObject", !PREVIEW)

    /**
     * System property {@code h2.bigDecimalIsDecimal}, {@code true} by default
     * unless {@code h2.preview} is enabled.
     * <p>
     * If {@code true} map {@code BigDecimal} to {@code DECIMAL} type.
     * </p>
     * <p>
     * If {@code false} map {@code BigDecimal} to {@code NUMERIC} as specified
     * in JDBC specification (see Mapping from JDBC Types to Java Object Types).
     * </p>
     */
    val BIG_DECIMAL_IS_DECIMAL: Boolean = getProperty("h2.bigDecimalIsDecimal", !PREVIEW)

    /**
     * System property {@code h2.returnOffsetDateTime}, {@code false} by default
     * unless {@code h2.preview} is enabled.
     * <p>
     * If {@code true} {@link java.sql.ResultSet#getObject(int)} and
     * {@link java.sql.ResultSet#getObject(string)} return
     * {@code TIMESTAMP WITH TIME ZONE} values as
     * {@code java.time.OffsetDateTime}.
     * </p>
     * <p>
     * If {@code false} return them as {@code org.h2.api.TimestampWithTimeZone} instead.
     * </p>
     * <p>
     * This property has effect only on Java 8 / Android API 26 and later versions.
     * Without JSR-130 {@code org.h2.api.TimestampWithTimeZone} is used unconditionally.
     * </p>
     */
    val RETURN_OFFSET_DATE_TIME: Boolean = getProperty("h2.returnOffsetDateTime", PREVIEW)

    /**
     * System property <code>h2.pgClientEncoding</code> (default: UTF-8).<br/>
     * Default client encoding for PG server. It is used if the client does not
     * sends his encoding.
     */
    val PG_DEFAULT_CLIENT_ENCODING: String = getProperty("h2.pgClientEncoding", "UTF-8")!!

    /**
     * System property <code>h2.prefixTempFile</code> (default: h2.temp).<br/>
     * The prefix for temporary files in the temp directory.
     */
    val PREFIX_TEMP_FILE: String = getProperty("h2.prefixTempFile", "h2.temp")!!

    /**
     * System property <code>h2.serverCachedObjects</code> (default: 64).<br/>
     * TCP Server: number of cached objects per session.
     */
    val SERVER_CACHED_OBJECTS: Int = getProperty("h2.serverCachedObjects", 64)

    /**
     * System property <code>h2.serverResultSetFetchSize</code>
     * (default: 100).<br/>
     * The default result set fetch size when using the server mode.
     */
    val SERVER_RESULT_SET_FETCH_SIZE: Int = getProperty("h2.serverResultSetFetchSize", 100)

    /**
     * System property <code>h2.socketConnectRetry</code> (default: 16).<br/>
     * The number of times to retry opening a socket. Windows sometimes fails
     * to open a socket, see bug.
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6213296
     */
    val SOCKET_CONNECT_RETRY: Int = getProperty("h2.socketConnectRetry", 16)

    /**
     * System property <code>h2.socketConnectTimeout</code>
     * (default: 2000).<br/>
     * The timeout in milliseconds to connect to a server.
     */
    val SOCKET_CONNECT_TIMEOUT: Int = getProperty("h2.socketConnectTimeout", 2000)

    /**
     * System property <code>h2.sortBinaryUnsigned</code>
     * (default: true).<br/>
     * Whether binary data should be sorted in unsigned mode
     * (0xff is larger than 0x00) by default in new databases.
     */
    val SORT_BINARY_UNSIGNED: Boolean = getProperty("h2.sortBinaryUnsigned", true)

    /**
     * System property {@code h2.sortUuidUnsigned}, {@code false} by default
     * unless {@code h2.preview} is enabled.
     * Whether UUID data should be sorted in unsigned mode
     * ('ffffffff-ffff-ffff-ffff-ffffffffffff' is larger than
     *  '00000000-0000-0000-0000-000000000000') by default in new databases.
     */
    val SORT_UUID_UNSIGNED: Boolean = getProperty("h2.sortUuidUnsigned", PREVIEW)

    /**
     * System property <code>h2.sortNullsHigh</code> (default: false).<br/>
     * Invert the default sorting behavior for NULL, such that NULL
     * is at the end of a result set in an ascending sort and at the
     * beginning of a result set in a descending sort.
     */
    val SORT_NULL_HIGH: Boolean = getProperty("h2.sortNullsHigh", false)

    /**
     * System property <code>h2.splitFileSizeShift</code> (default: 30).<br/>
     * The maximum file size of a split file is 1L &lt;&lt; x.
     */
    val SPLIT_FILE_SIZE_SHIFT: Long = getProperty("h2.splitFileSizeShift", 30).toLong()

    /**
     * System property <code>h2.syncMethod</code> (default: sync).<br/>
     * What method to call when closing the database, on checkpoint, and on
     * CHECKPOINT SYNC. The following options are supported:
     * "sync" (default): RandomAccessFile.getFD().sync();
     * "force": RandomAccessFile.getChannel().force(true);
     * "forceFalse": RandomAccessFile.getChannel().force(false);
     * "": do not call a method (fast but there is a risk of data loss on power failure).
     */
    val SYNC_METHOD: String = getProperty("h2.syncMethod", "sync")!!

    /**
     * System property <code>h2.traceIO</code> (default: false).<br/>
     * Trace all I/O operations.
     */
    val TRACE_IO: Boolean = getProperty("h2.traceIO", false)

    /**
     * System property <code>h2.threadDeadlockDetector</code>
     * (default: false).<br/>
     * Detect thread deadlocks in a background thread.
     */
    val THREAD_DEADLOCK_DETECTOR: Boolean = getProperty("h2.threadDeadlockDetector", false)

    /**
     * System property <code>h2.implicitRelativePath</code>
     * (default: false). <br/>
     * If disabled, relative paths in database URLs need to be written as
     * jdbc:h2:./test instead of jdbc:h2:test.
     */
    val IMPLICIT_RELATIVE_PATH: Boolean = getProperty("h2.implicitRelativePath", false)

    /**
     * System property <code>h2.urlMap</code> (default: null).<br/>
     * A properties file that contains a mapping between database URLs. New
     * connections are written into the file. An empty value in the map means no
     * redirection is used for the given URL.
     */
    val URL_MAP: String? = getProperty("h2.urlMap", null)

    /**
     * System property <code>h2.useThreadContextClassLoader</code>
     * (default: false).<br/>
     * Instead of using the default class loader when deserializing objects, the
     * current thread-context class loader will be used.
     */
    val USE_THREAD_CONTEXT_CLASS_LOADER: Boolean = getProperty("h2.useThreadContextClassLoader", false)

    /**
     * System property <code>h2.serializeJavaObject</code>
     * (default: true). <br/>
     * <b>If true</b>, values of type OTHER will be stored in serialized form
     * and have the semantics of binary data for all operations (such as sorting
     * and conversion to string).
     * <br/>
     */
    var serializeJavaObject: Boolean = getProperty("h2.serializeJavaObject", true)

    /**
     * System property <code>h2.javaObjectSerializer</code>
     * (default: null).<br/>
     * The JavaObjectSerializer class name for java objects being stored in column
     * of type OTHER. It must be the same on client and server to work correctly.
     */
    val JAVA_OBJECT_SERIALIZER: String? = getProperty("h2.javaObjectSerializer", null)

    /**
     * System property <code>h2.customDataTypesHandler</code>
     * (default: null).<br/>
     * The customDataTypesHandler class name that is used to provide
     * support for user defined custom data types.
     * It must be the same on client and server to work correctly.
     */
    val CUSTOM_DATA_TYPES_HANDLER: String? = getProperty("h2.customDataTypesHandler", null)

    /**
     * System property <code>h2.authConfigFile</code>
     * (default: null). <br/>
     * authConfigFile define the URL of configuration file
     * of {@link org.h2.security.auth.DefaultAuthenticator}
     */
    val AUTH_CONFIG_FILE: String? = getProperty("h2.authConfigFile", null)

    private var H2_BASE_DIR: String = "h2.baseDir"
    private var baseDir: String?
        set(dir: String?) {
            System.setProperty(H2_BASE_DIR, dir!!.suffixIfNot("/"))
        }
        get() = getProperty(H2_BASE_DIR, null)


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
