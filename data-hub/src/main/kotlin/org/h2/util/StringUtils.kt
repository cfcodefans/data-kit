package org.h2.util

import java.lang.StringBuilder

object StringUtils {
    /**
     * Enclose a string with double quotes. A double quote inside the string is
     * escaped using a double quote.
     *
     * @param s the text
     * @return the double quoted text
     */
    fun quotedIdentifier(s: String): String {
        return quoteIdentifier(StringBuilder(s.length + 2), s).toString()
    }

    /**
     * Enclose a string with double quotes and append it to the specified
     * string builder. A double quote inside the string is escaped using a
     * double quote.
     *
     * @param builder string builder to append to
     * @param s the text
     * @return the specified builder
     */
    fun quoteIdentifier(builder: StringBuilder, s: String): StringBuilder {
        builder.append('"')
        for (c in s) {
            if (c == '"')
                builder.append(c)
            builder.append(c)
        }
        return builder.append('"')
    }
}