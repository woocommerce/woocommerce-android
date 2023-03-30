package com.woocommerce.android.extensions

import org.apache.commons.text.StringEscapeUtils

/**
 * Checks if a given string is a number (supports positive or negative numbers)
 */
fun String?.isNumeric() = this?.toIntOrNull()?.let { true } ?: false

/**
 * Checks if a given string is a Float
 */
fun String?.isFloat() = this?.toFloatOrNull()?.let { true } ?: false

/**
 * If the provided [line] is not null and not empty, then add the string to this instance. Will prepend the
 * [separator] if the current contents of this StringBuilder are not empty.
 */
fun StringBuilder.appendWithIfNotEmpty(line: String?, separator: String = ", "): StringBuilder {
    return line?.takeIf { it.isNotEmpty() }?.let {
        if (isNotEmpty()) {
            append(separator)
        }
        append(it)
    } ?: this
}

/**
 * This is much faster than HtmlCompat.fromHtml but should only be used when we know the html is valid
 * since the regex will be unpredictable with invalid html
 * String param containing only valid html
 * @return String without HTML
 *
 * Replicated from HtmlUtils.fastStripHtml and HtmlUtils.trimStart
 */
fun String.fastStripHtml(): String {
    // insert a line break before P tags unless the only one is at the start
    var str = this
    if (str.isEmpty()) return str

    if (str.lastIndexOf("<p") > 0) {
        str = str.replace(Regex("<p(.|\n)*?>"), "\n<p>")
    }

    // convert BR tags to line breaks
    if (str.contains("<br")) {
        str = str.replace(Regex("<br(.|\n)*?>"), "\n")
    }

    val htmlString = StringEscapeUtils.unescapeHtml4(str.replace(Regex("<(.|\n)*?>"), ""))
    if (htmlString.isEmpty()) return str

    var start = 0
    while (start != 0 && (Character.isWhitespace(htmlString[start]) || htmlString[start].code == 160)) {
        start++
    }

    // use regex to strip tags, then convert entities in the result
    return htmlString.substring(start)
}

fun String.semverCompareTo(otherVersion: String): Int {
    try {
        val thisVersionTokens = substringBefore("-").split(".").map { Integer.parseInt(it) }
        val otherVersionTokens = otherVersion.substringBefore("-").split(".").map { Integer.parseInt(it) }

        thisVersionTokens.forEachIndexed { index, token ->
            if (token > otherVersionTokens[index]) {
                return 1
            } else if (token < otherVersionTokens[index]) {
                return -1
            }
        }
        return 0
    } catch (e: NumberFormatException) {
        // if the parsing fails, consider this version lower than the other one
        return -1
    }
}

fun String.capitalizeWords(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
}

/**
 * Returns this string if it's not empty or null otherwise.
 * Syntactic sugar for `string.ifEmpty { null }`.
 */
fun String.orNullIfEmpty(): String? = this.ifEmpty { null }

fun String?.isNotNullOrEmpty() = this.isNullOrEmpty().not()
