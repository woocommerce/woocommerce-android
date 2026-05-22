package com.woocommerce.commons.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink

internal object MarkdownParser {
    fun parse(
        markdown: String,
        linkStyles: TextLinkStyles? = null,
        linkInteractionListener: LinkInteractionListener? = null,
    ): AnnotatedString = buildAnnotatedString {
        appendMarkdown(
            markdown = markdown,
            linkStyles = linkStyles,
            linkInteractionListener = linkInteractionListener,
            parseLinks = true,
        )
    }

    private fun AnnotatedString.Builder.appendMarkdown(
        markdown: String,
        linkStyles: TextLinkStyles?,
        linkInteractionListener: LinkInteractionListener?,
        parseLinks: Boolean,
    ) {
        var index = 0
        while (index < markdown.length) {
            index = appendNextMarkdownToken(
                markdown = markdown,
                index = index,
                linkStyles = linkStyles,
                linkInteractionListener = linkInteractionListener,
                parseLinks = parseLinks,
            )
        }
    }

    private fun AnnotatedString.Builder.appendNextMarkdownToken(
        markdown: String,
        index: Int,
        linkStyles: TextLinkStyles?,
        linkInteractionListener: LinkInteractionListener?,
        parseLinks: Boolean,
    ): Int = when {
        parseLinks && markdown.startsWith(LINK_LABEL_START, index) -> {
            appendLinkOrNull(
                markdown = markdown,
                startIndex = index,
                linkStyles = linkStyles,
                linkInteractionListener = linkInteractionListener,
            ) ?: appendCharacter(markdown, index)
        }
        markdown.startsWith(BOLD_ITALIC_DELIMITER, index) -> appendStyledTextOrRemaining(
            markdown = markdown,
            startIndex = index,
            delimiter = BOLD_ITALIC_DELIMITER,
            style = SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
        )
        markdown.startsWith(BOLD_DELIMITER, index) -> appendStyledTextOrRemaining(
            markdown = markdown,
            startIndex = index,
            delimiter = BOLD_DELIMITER,
            style = SpanStyle(fontWeight = FontWeight.Bold),
        )
        markdown.startsWith(ITALIC_DELIMITER, index) -> appendStyledTextOrRemaining(
            markdown = markdown,
            startIndex = index,
            delimiter = ITALIC_DELIMITER,
            style = SpanStyle(fontStyle = FontStyle.Italic),
        )
        else -> appendCharacter(markdown, index)
    }

    private fun AnnotatedString.Builder.appendStyledTextOrRemaining(
        markdown: String,
        startIndex: Int,
        delimiter: String,
        style: SpanStyle,
    ): Int = appendStyledTextOrNull(
        markdown = markdown,
        startIndex = startIndex,
        delimiter = delimiter,
        style = style,
    ) ?: appendRemaining(markdown, startIndex)

    private fun AnnotatedString.Builder.appendStyledTextOrNull(
        markdown: String,
        startIndex: Int,
        delimiter: String,
        style: SpanStyle,
    ): Int? {
        val contentStart = startIndex + delimiter.length
        val endIndex = markdown.indexOf(delimiter, startIndex = contentStart)
        if (endIndex == -1) return null

        val start = length
        append(markdown.substring(contentStart, endIndex))
        addStyle(style, start, length)
        return endIndex + delimiter.length
    }

    private fun AnnotatedString.Builder.appendCharacter(markdown: String, index: Int): Int {
        append(markdown[index])
        return index + 1
    }

    private fun AnnotatedString.Builder.appendRemaining(markdown: String, index: Int): Int {
        append(markdown.substring(index))
        return markdown.length
    }

    private fun AnnotatedString.Builder.appendLinkOrNull(
        markdown: String,
        startIndex: Int,
        linkStyles: TextLinkStyles?,
        linkInteractionListener: LinkInteractionListener?,
    ): Int? {
        val labelEnd = markdown.indexOf(LINK_LABEL_END_AND_URL_START, startIndex = startIndex + 1)
        if (labelEnd == -1) return null

        val urlStart = labelEnd + LINK_LABEL_END_AND_URL_START.length
        val urlEnd = markdown.indexOf(LINK_URL_END, startIndex = urlStart)
        if (urlEnd == -1) return null

        val label = markdown.substring(startIndex + 1, labelEnd)
        val url = markdown.substring(urlStart, urlEnd)
        if (label.isBlank() || url.isBlank() || !isAllowedScheme(url)) return null

        val linkAnnotation = LinkAnnotation.Url(
            url = url,
            styles = linkStyles,
            linkInteractionListener = linkInteractionListener,
        )
        withLink(linkAnnotation) {
            appendMarkdown(
                markdown = label,
                linkStyles = null,
                linkInteractionListener = null,
                parseLinks = false,
            )
        }
        return urlEnd + 1
    }

    private fun isAllowedScheme(url: String): Boolean {
        val scheme = url.substringBefore(':', "").lowercase()
        return scheme == "http" || scheme == "https"
    }

    private const val LINK_LABEL_START = "["
    private const val LINK_LABEL_END_AND_URL_START = "]("
    private const val LINK_URL_END = ')'
    private const val BOLD_ITALIC_DELIMITER = "***"
    private const val BOLD_DELIMITER = "**"
    private const val ITALIC_DELIMITER = "*"
}
