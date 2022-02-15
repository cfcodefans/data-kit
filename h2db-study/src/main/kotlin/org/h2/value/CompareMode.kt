package org.h2.value

import org.h2.util.StringUtils
import java.nio.charset.Charset
import java.text.Collator
import java.util.Locale
import java.util.Objects

/**
 * Instances of this class can compare strings. Case sensitive and case
 * insensitive comparison is supported, and comparison using a collator.
 */
open class CompareMode(val name: String = OFF, val strength: Int) : Comparator<Value> {
    companion object {
        /**
         * This constant means there is no collator set, and the default string
         * comparison is to be used.
         */
        const val OFF = "OFF"

        /**
         * This constant means the default collator should be used, even if ICU4J is
         * in the classpath.
         */
        const val DEFAULT = "DEFAULT_"

        /**
         * This constant means ICU4J should be used (this will fail if it is not in
         * the classpath).
         */
        const val ICU4J = "ICU4J_"

        /**
         * This constant means the charset specified should be used.
         * This will fail if the specified charset does not exist.
         */
        const val CHARSET = "CHARSET_"

        private val LOCALES: Array<Locale> by lazy { Collator.getAvailableLocales() }

        @Volatile
        private var lastUsed: CompareMode? = null

        private var CAN_USE_ICU4J: Boolean = kotlin.runCatching { Class.forName("com.ibm.icu.text.Collator") }.isSuccess

        /**
         * Get the collation name.
         *
         * @param l the locale
         * @return the name of the collation
         */
        fun getName(l: Locale): String? {
            val english = Locale.ENGLISH
            return "${l.getDisplayLanguage(english)} ${l.getDisplayCountry(english)} ${l.variant}"
                    .trim()
                    .replace(' ', '_')
                    .let { StringUtils.toUpperEnglish(it) }
        }

        /**
         * Compare name of the locale with the given name. The case of the name
         * is ignored.
         *
         * @param locale the locale
         * @param name the name
         * @return true if they match
         */
        fun compareLocaleNames(locale: Locale, name: String) =
                name.equals(locale.toString(), ignoreCase = true)
                        || name.equals(locale.toLanguageTag(), ignoreCase = true)
                        || name.equals(getName(locale), ignoreCase = true)

        /**
         * Create a new compare mode with the given collator and strength. If
         * required, a new CompareMode is created, or if possible the last one is
         * returned. A cache is used to speed up comparison when using a collator;
         * CollationKey objects are cached.
         *
         * @param name the collation name or null
         * @param strength the collation strength
         * @return the compare mode
         */
        fun getInstance(name: String?, strength: Int): CompareMode {
            var last = lastUsed
            if (last != null && last.name == name && last.strength == strength) return last

            lastUsed = if (name == null || name == OFF) {
                CompareMode(name!!, strength)
            } else {
                if (name.startsWith(ICU4J) || CAN_USE_ICU4J) {
                    CompareModeIcu4J(name.removePrefix(ICU4J), strength)
                } else {
                    CompareModeDefault(name.removePrefix(DEFAULT), strength)
                }
            }
            return lastUsed!!
        }


        /**
         * Get the collator object for the given language name or language / country
         * combination.
         *
         * @param name the language name
         * @return the collator
         */
        open fun getCollator(name: String): Collator? {
            if (name.startsWith(CHARSET)) {
                return CharsetCollator(Charset.forName(name.substring(CHARSET.length)))
            }

            var result: Collator? = null
            var name = when {
                name.startsWith(ICU4J) -> name.substring(ICU4J.length)
                name.startsWith(DEFAULT) -> name.substring(DEFAULT.length)
                else -> name
            }

            val length = name.length
            if (length == 2) {
                val locale = Locale(StringUtils.toLowerEnglish(name), "")
                if (compareLocaleNames(locale, name)) {
                    result = Collator.getInstance(locale)
                }
            } else if (length == 5) { // LL_CC (language_country)
                val idx = name.indexOf('_')
                if (idx >= 0) {
                    val language = StringUtils.toLowerEnglish(name.substring(0, idx))
                    val country = name.substring(idx + 1)
                    val locale = Locale(language, country)
                    if (compareLocaleNames(locale, name)) {
                        result = Collator.getInstance(locale)
                    }
                }
            } else if (name.indexOf('-') > 0) {
                val locale = Locale.forLanguageTag(name)
                if (locale.language.isNotEmpty()) return Collator.getInstance(locale)
            }

            return result ?: LOCALES
                    .firstOrNull { locale -> compareLocaleNames(locale, name) }
                    ?.let { locale -> Collator.getInstance(locale) }
        }


    }

    /**
     * Compare two characters in a string.
     *
     * @param a the first string
     * @param ai the character index in the first string
     * @param b the second string
     * @param bi the character index in the second string
     * @param ignoreCase true if a case-insensitive comparison should be made
     * @return true if the characters are equals
     */
    open fun equalsChars(a: String, ai: Int, b: String, bi: Int, ignoreCase: Boolean): Boolean {
        val ca = a[ai]
        val cb = b[bi]
        if (ca == cb) return true

        return (ignoreCase
                && (ca.uppercaseChar() == cb.uppercaseChar()
                || ca.lowercaseChar() == cb.lowercaseChar()))
    }

    /**
     * Compare two strings.
     *
     * @param a the first string
     * @param b the second string
     * @param ignoreCase true if a case-insensitive comparison should be made
     * @return -1 if the first string is 'smaller', 1 if the second string is
     * smaller, and 0 if they are equal
     */
    open fun compareString(a: String, b: String?, ignoreCase: Boolean): Int = a.compareTo(b!!, ignoreCase = ignoreCase)

    override fun hashCode(): Int = Objects.hash(name, strength)

    override fun equals(other: Any?): Boolean = (other === this)
            || (other is CompareMode
            && name == other.name
            && strength == other.strength)

    override fun compare(o1: Value, o2: Value): Int = o1.compareTo(o2, null, this)
}