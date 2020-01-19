package org.h2.engine

import org.h2.util.MathUtils
import org.h2.util.Utils
import org.h2.util.Utils.getProperty
import org.jetbrains.kotlin.konan.file.File

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
}