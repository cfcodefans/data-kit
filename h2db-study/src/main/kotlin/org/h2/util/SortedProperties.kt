package org.h2.util

import org.h2.store.fs.FileUtils
import org.h2.util.Utils.parseBoolean
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.LineNumberReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.util.Enumeration
import java.util.Properties
import java.util.TreeMap
import java.util.Vector

/**
 * Sorted properties file.
 * This implementation requires that org.h2.store() internally calls keys().
 */
class SortedProperties : Properties() {
    companion object {
        @JvmStatic
        val serialVersionUID: Long = 1L

        /**
         * Convert a string to a map
         *
         * @param s the string
         * @return the map
         */
        @JvmStatic
        fun fromLines(s: String): SortedProperties {
            val p: SortedProperties = SortedProperties()
            for (line in s.split("\n")) {
                val idx: Int = line.indexOf('=')
                if (idx > 0) {
                    p[line.substring(0, idx)] = line.substring(idx + 1)
                }
            }
            return p
        }

        /**
         * Load a properties object from a file.
         *
         * @param fileName the name of the properties file
         * @return the properties object
         * @throws IOException on failure
         */
        @Synchronized
        @Throws(IOException::class)
        fun loadProperties(fileName: String?): SortedProperties {
            val prop = SortedProperties()
            if (FileUtils.exists(fileName!!)) {
                FileUtils.newInputStream(fileName).use { `in` -> prop.load(`in`) }
            }
            return prop
        }
    }

    @Synchronized
    override fun keys(): Enumeration<Any> = Vector(super.keys.map { it.toString() }.sorted()).elements().cast()

    /**
     * Get a boolean property value from a properties object.
     *
     * @param prop the properties object
     * @param key the key
     * @param def the default value
     * @return the value if set, or the default value if not
     */
    fun getBooleanProperty(prop: Properties, key: String?, def: Boolean): Boolean = try {
        parseBoolean(prop.getProperty(key, null), def, true)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        def
    }

    /**
     * Get an int property value from a properties object.
     *
     * @param prop the properties object
     * @param key the key
     * @param def the default value
     * @return the value if set, or the default value if not
     */
    fun getIntProperty(prop: Properties, key: String?, def: Int): Int = try {
        Integer.decode(prop.getProperty(key, def.toString()))
    } catch (e: Exception) {
        e.printStackTrace()
        def
    }

    /**
     * Get a string property value from a properties object.
     *
     * @param prop the properties object
     * @param key the key
     * @param def the default value
     * @return the value if set, or the default value if not
     */
    fun getStringProperty(prop: Properties, key: String?, def: String?): String? {
        return prop.getProperty(key, def)
    }

    /**
     * Store a properties file. The header and the date is not written.
     *
     * @param fileName the target file name
     * @throws IOException on failure
     */
    @Synchronized
    @Throws(IOException::class)
    fun store(fileName: String?) {
        val out = ByteArrayOutputStream()
        store(out, null)

        val `in` = ByteArrayInputStream(out.toByteArray())
        val reader = InputStreamReader(`in`, StandardCharsets.ISO_8859_1)
        val r = LineNumberReader(reader)
        val w: Writer = try {
            OutputStreamWriter(FileUtils.newOutputStream(fileName!!, false))
        } catch (e: Exception) {
            throw IOException(e.toString(), e)
        }

        PrintWriter(BufferedWriter(w)).use { writer ->
            r.forEachLine { line ->
                if (line.startsWith("#")) return@forEachLine
                writer.print("$line\n")
            }
        }
    }

    /**
     * Convert the map to a list of line in the form key=value.
     * @return the lines
     */
    @Synchronized
    fun toLines(): String = TreeMap(this).entries.joinToString { en -> "${en.key}=${en.value}\n" }

}