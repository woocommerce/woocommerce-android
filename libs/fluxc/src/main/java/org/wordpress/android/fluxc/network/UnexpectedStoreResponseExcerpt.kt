package org.wordpress.android.fluxc.network

import org.apache.commons.text.StringEscapeUtils

object UnexpectedStoreResponseExcerpt {
    fun from(body: String): String? {
        val title = TITLE_PATTERN.find(body)?.groupValues?.get(1)?.visibleText()?.cleaned().orEmpty()
        val text = body.replace(NON_RENDERED_REGION_PATTERN, " ").visibleText().substringBeforeJson().cleaned()
        val excerpt = when {
            title.isEmpty() || text.startsWith(title) -> text
            text.isEmpty() -> title
            else -> "$title | $text"
        }
        return excerpt.masked().truncated().ifEmpty { null }
    }

    private fun String.visibleText(): String {
        val text = replace(TAG_PATTERN, " ").replace(WHITESPACE_PATTERN, " ").trim()
        return if (text.length > MAX_CLEANED_LENGTH) {
            text.take(MAX_CLEANED_LENGTH).substringBeforeLast(' ')
        } else {
            text
        }
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
    private val NON_RENDERED_REGION_PATTERN = Regex(
        pattern = """<!--.*?(?:-->|\z)|<head\b[^<>]*>.*?(?:</head\s*>|(?=<body\b)|\z)|""" +
            """<(script|style|noscript|template|svg|iframe)\b[^<>]*>.*?(?:</\1\s*>|\z)""",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val TAG_PATTERN = Regex("""<[^<>]*>""")
    private val WHITESPACE_PATTERN = Regex("""\s+""")
    private val EMAIL_PATTERN = Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}""")
    private val IPV4_PATTERN = Regex("""\b(?:\d{1,3}\.){3}\d{1,3}\b""")
    private val IPV6_PATTERN = Regex(
        pattern = """(?<![0-9a-f:])(?:(?:[0-9a-f]{1,4}:){7}[0-9a-f]{1,4}|""" +
            """(?:[0-9a-f]{1,4}:){1,6}:(?:[0-9a-f]{1,4}:){0,5}[0-9a-f]{1,4})(?![0-9a-f:])""",
        option = RegexOption.IGNORE_CASE
    )
}
