package org.wordpress.android.fluxc.network

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.apache.commons.text.StringEscapeUtils
import java.util.Locale

object UnexpectedStoreResponseExcerpt {
    fun from(body: String): String? {
        val excerpt = jsonErrorText(body) ?: pageText(body)
        return excerpt.masked().truncated().ifEmpty { null }
    }

    private fun pageText(body: String): String {
        val visibleBody = body.replace(HIDDEN_REGION_PATTERN, " ").withoutHiddenElements()
        val title = TITLE_PATTERN.find(visibleBody)?.groupValues?.get(1)?.visibleText()?.cleaned().orEmpty()
        val text = visibleBody.replace(HEAD_PATTERN, " ").visibleText().substringBeforeJson().cleaned()
        return when {
            title.isEmpty() || text.startsWith(title) -> text
            text.isEmpty() -> title
            else -> "$title | $text"
        }
    }

    private fun jsonErrorText(body: String): String? {
        val error = body.takeIf { it.trimStart().startsWith("{") }?.toJsonObjectOrNull()
        val code = error?.stringOrNull("code")
        val message = error?.stringOrNull("message")
        return if (code != null && message != null) "$code | ${message.visibleText()}".cleaned() else null
    }

    @Suppress("SwallowedException")
    private fun String.toJsonObjectOrNull(): JsonObject? = try {
        JsonParser.parseString(this).takeIf { it.isJsonObject }?.asJsonObject
    } catch (e: JsonParseException) {
        null
    }

    private fun JsonObject.stringOrNull(name: String): String? =
        get(name)?.takeIf { it.isJsonPrimitive }?.asString

    private fun String.visibleText(): String {
        val text = withoutTags().replace(WHITESPACE_PATTERN, " ").trim()
        return if (text.length > MAX_CLEANED_LENGTH) {
            text.take(MAX_CLEANED_LENGTH).substringBeforeLast(' ')
        } else {
            text
        }
    }

    /**
     * Drops the elements a page marks as hidden, with everything inside them. One that is never closed drops the rest
     * of the page. Elements hidden by a stylesheet or a script can't be told apart here.
     */
    private fun String.withoutHiddenElements(): String {
        val result = StringBuilder()
        var index = 0
        while (index < length) {
            val tag = nextTag(index)
            if (tag == null || !tag.isHiddenStart) {
                val end = tag?.end ?: length
                result.append(this, index, end)
                index = end
            } else {
                result.append(this, index, tag.start).append(' ')
                index = if (tag.isEmpty) tag.end else elementEnd(tag) ?: length
            }
        }
        return result.toString()
    }

    private fun String.nextTag(from: Int): Tag? {
        val start = nextTagStart(from) { hasTagNameAt(it) } ?: return null
        val isClosing = this[start + 1] == '/'
        val nameStart = if (isClosing) start + 2 else start + 1
        val nameEnd = tagNameEnd(nameStart)
        val end = tagEnd(nameEnd) ?: return null
        return Tag(
            name = substring(nameStart, nameEnd).lowercase(Locale.ROOT),
            isClosing = isClosing,
            attributes = substring(nameEnd, end - 1),
            start = start,
            end = end
        )
    }

    private fun String.elementEnd(element: Tag): Int? {
        var depth = 1
        var index = element.end
        while (depth > 0) {
            val tag = nextTag(index) ?: return null
            if (tag.name == element.name && !tag.isEmpty) depth += if (tag.isClosing) -1 else 1
            index = tag.end
        }
        return index
    }

    private class Tag(val name: String, val isClosing: Boolean, attributes: String, val start: Int, val end: Int) {
        val isEmpty = name in VOID_ELEMENTS || attributes.trimEnd().endsWith('/')
        val isHiddenStart = !isClosing && (attributes.hasHiddenAttribute() || attributes.hasHiddenStyle())
    }

    private fun String.hasHiddenAttribute(): Boolean =
        contains("hidden", ignoreCase = true) &&
            HIDDEN_ATTRIBUTE_PATTERN.containsMatchIn(replace(QUOTED_VALUE_PATTERN, ""))

    private fun String.hasHiddenStyle(): Boolean =
        contains("style", ignoreCase = true) && HIDDEN_STYLE_PATTERN.containsMatchIn(this)

    /**
     * Replaces each tag with a space. A tag that is never closed drops the rest of the page.
     */
    private fun String.withoutTags(): String {
        val result = StringBuilder()
        var index: Int? = 0
        while (index != null && index < length) {
            val tagStart = nextTagStart(index) { hasTagOpenAt(it) } ?: length
            result.append(this, index, tagStart).append(' ')
            index = if (tagStart < length) tagEnd(tagStart + 1) else null
        }
        return result.toString()
    }

    /**
     * Returns the index after the `>` that ends a tag, so a `>` inside a quoted attribute value doesn't end it.
     */
    private fun String.tagEnd(from: Int): Int? {
        var quote: Char? = null
        for (index in from until length) {
            val char = this[index]
            when {
                quote != null -> if (char == quote) quote = null
                char == '"' || char == '\'' -> quote = char
                char == '>' -> return index + 1
            }
        }
        return null
    }

    /**
     * Uses `indexOf` because on Android each regex search from an index copies the whole string, which blocks the
     * thread on a page with many tags.
     */
    private inline fun String.nextTagStart(from: Int, isTagStart: (Int) -> Boolean): Int? {
        var index = indexOf('<', from)
        while (index != -1 && !isTagStart(index)) {
            index = indexOf('<', index + 1)
        }
        return index.takeIf { it != -1 }
    }

    private fun String.hasTagOpenAt(index: Int): Boolean =
        getOrNull(index + 1)?.let { it.isAsciiLetter() || it in TAG_OPEN_SYMBOLS } == true

    private fun String.hasTagNameAt(index: Int): Boolean {
        val nameStart = if (getOrNull(index + 1) == '/') index + 2 else index + 1
        return getOrNull(nameStart)?.isAsciiLetter() == true
    }

    private fun String.tagNameEnd(from: Int): Int {
        var index = from
        while (index < length && this[index].isTagNameCharacter()) {
            index++
        }
        return index
    }

    private fun Char.isTagNameCharacter(): Boolean = isAsciiLetter() || this in '0'..'9' || this == '-'

    private fun Char.isAsciiLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

    private fun String.substringBeforeJson(): String {
        val jsonStart = indexOfFirst { it == '{' || it == '[' }
        return if (jsonStart == -1) this else take(jsonStart).trimEnd()
    }

    private fun String.cleaned(): String = SensitiveDataSanitizer.sanitize(unescapedHtml())

    @Suppress("SwallowedException")
    private fun String.unescapedHtml(): String = try {
        StringEscapeUtils.unescapeHtml4(this)
    } catch (e: IllegalArgumentException) {
        this
    }

    private fun String.masked(): String =
        replace(EMAIL_PATTERN, "[email]").replace(IPV4_PATTERN, "[ip]").replace(IPV6_PATTERN, "[ip]")

    private fun String.truncated(): String =
        if (length > MAX_LENGTH) take(MAX_LENGTH - ELLIPSIS.length).trimEnd() + ELLIPSIS else this

    private const val MAX_LENGTH = 300
    private const val ELLIPSIS = "…"
    private const val MAX_CLEANED_LENGTH = 4096
    private const val TAG_OPEN_SYMBOLS = "/!?"
    private val TITLE_PATTERN = Regex(
        pattern = """<title\b[^<>]*>([^<]*)</title\s*>""",
        option = RegexOption.IGNORE_CASE
    )
    private val HIDDEN_REGION_PATTERN = Regex(
        pattern = """<!--.*?(?:-->|\z)|<(script|style|noscript|template|svg|iframe)\b[^<>]*>.*?(?:</\1\s*>|\z)""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HEAD_PATTERN = Regex(
        pattern = """<head\b[^<>]*>.*?(?:</head\s*>|(?=<body\b)|\z)""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val QUOTED_VALUE_PATTERN = Regex(""""[^"]*"|'[^']*'""")
    private val HIDDEN_ATTRIBUTE_PATTERN = Regex("""(?:^|\s)hidden(?=[\s=/]|$)""", RegexOption.IGNORE_CASE)
    private val HIDDEN_STYLE_PATTERN = Regex(
        pattern = """(?:^|\s)style\s*=\s*(?:"[^"]*|'[^']*)(?:display\s*:\s*none|visibility\s*:\s*hidden)""",
        option = RegexOption.IGNORE_CASE
    )
    private val VOID_ELEMENTS = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "source", "track", "wbr"
    )
    private val WHITESPACE_PATTERN = Regex("""\s+""")
    private val EMAIL_PATTERN = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}""")
    private val IPV4_PATTERN = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val IPV6_PATTERN = Regex(
        pattern = """(?<![0-9a-f:])(?:(?:[0-9a-f]{1,4}:){7}[0-9a-f]{1,4}|""" +
            """(?:[0-9a-f]{1,4}:){1,6}:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})(?![0-9a-f:])""",
        option = RegexOption.IGNORE_CASE
    )
}
