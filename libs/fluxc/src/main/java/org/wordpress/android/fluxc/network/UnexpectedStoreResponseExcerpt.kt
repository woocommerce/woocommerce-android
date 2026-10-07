package org.wordpress.android.fluxc.network

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import org.apache.commons.text.StringEscapeUtils

object UnexpectedStoreResponseExcerpt {
    fun from(body: String): String? {
        val excerpt = jsonErrorText(body) ?: pageText(body)
        return excerpt.masked().truncated().ifEmpty { null }
    }

    private fun pageText(body: String): String {
        val visibleBody = body.replace(HIDDEN_REGION_PATTERN, " ")
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
     * Replaces each tag with a space. A tag that is never closed drops the rest of the page.
     */
    private fun String.withoutTags(): String {
        val result = StringBuilder()
        var index: Int? = 0
        while (index != null && index < length) {
            val tagStart = TAG_OPEN_PATTERN.find(this, index)?.range?.first ?: length
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
    private val TAG_OPEN_PATTERN = Regex("""<[A-Za-z/!?]""")
    private val WHITESPACE_PATTERN = Regex("""\s+""")
    private val EMAIL_PATTERN = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}""")
    private val IPV4_PATTERN = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val IPV6_PATTERN = Regex(
        pattern = """(?<![0-9a-f:])(?:(?:[0-9a-f]{1,4}:){7}[0-9a-f]{1,4}|""" +
            """(?:[0-9a-f]{1,4}:){1,6}:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})(?![0-9a-f:])""",
        option = RegexOption.IGNORE_CASE
    )
}
