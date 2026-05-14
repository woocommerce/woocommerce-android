package com.woocommerce.android.aiassistant.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink

internal object AssistantMarkdownParser {
    fun parse(
        markdown: String,
        linkStyles: TextLinkStyles? = null,
        linkInteractionListener: LinkInteractionListener? = null,
    ): AnnotatedString = buildAnnotatedString {
        var index = 0
        while (index < markdown.length) {
            when {
                markdown.startsWith(LINK_LABEL_START, index) -> {
                    val parsed = appendLinkOrNull(
                        markdown = markdown,
                        startIndex = index,
                        linkStyles = linkStyles,
                        linkInteractionListener = linkInteractionListener,
                    )
                    if (parsed != null) {
                        index = parsed
                    } else {
                        append(markdown[index])
                        index += 1
                    }
                }
                markdown.startsWith(BOLD_DELIMITER, index) -> {
                    val endIndex = markdown.indexOf(BOLD_DELIMITER, startIndex = index + BOLD_DELIMITER.length)
                    if (endIndex == -1) {
                        append(markdown.substring(index))
                        index = markdown.length
                    } else {
                        val start = length
                        append(markdown.substring(index + BOLD_DELIMITER.length, endIndex))
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                        index = endIndex + BOLD_DELIMITER.length
                    }
                }
                markdown[index] == ITALIC_DELIMITER -> {
                    val endIndex = markdown.indexOf(ITALIC_DELIMITER, startIndex = index + 1)
                    if (endIndex == -1) {
                        append(markdown.substring(index))
                        index = markdown.length
                    } else {
                        val start = length
                        append(markdown.substring(index + 1, endIndex))
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        index = endIndex + 1
                    }
                }
                else -> {
                    append(markdown[index])
                    index += 1
                }
            }
        }
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
        if (label.isBlank() || url.isBlank()) return null

        val linkAnnotation = LinkAnnotation.Url(
            url = url,
            styles = linkStyles,
            linkInteractionListener = linkInteractionListener,
        )
        withLink(linkAnnotation) {
            append(label)
        }
        return urlEnd + 1
    }

    private const val LINK_LABEL_START = "["
    private const val LINK_LABEL_END_AND_URL_START = "]("
    private const val LINK_URL_END = ')'
    private const val BOLD_DELIMITER = "**"
    private const val ITALIC_DELIMITER = '*'
}
