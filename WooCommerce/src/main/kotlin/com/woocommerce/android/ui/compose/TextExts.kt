package com.woocommerce.android.ui.compose

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

const val URL_ANNOTATION_TAG = "url"

/**
 * Creates an [AnnotatedString] from the passed String resource.
 *
 * Source: https://stackoverflow.com/a/70757314 with some adjustments
 */
@Composable
fun annotatedStringResLegacy(@StringRes stringResId: Int, vararg args: Any): AnnotatedString {
    val string = stringResource(id = stringResId, *args)
    val spanned = HtmlCompat.fromHtml(string, HtmlCompat.FROM_HTML_MODE_LEGACY)

    return buildAnnotatedString {
        append(spanned.toString())

        for (span in spanned.getSpans(0, spanned.length, Any::class.java)) {
            val startIndex = spanned.getSpanStart(span)
            val endIndex = spanned.getSpanEnd(span)

            when (span) {
                is StyleSpan -> span.toSpanStyle()?.let {
                    addStyle(style = it, start = startIndex, end = endIndex)
                }

                is UnderlineSpan -> {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start = startIndex, end = endIndex)
                }

                is URLSpan -> {
                    addStyle(
                        style = TextStyle.Default.copy(color = MaterialTheme.colors.primary).toSpanStyle(),
                        start = startIndex,
                        end = endIndex
                    )
                    addStringAnnotation(
                        tag = URL_ANNOTATION_TAG,
                        annotation = span.url,
                        start = startIndex,
                        end = endIndex
                    )
                }
            }
        }
    }
}

/**
 * Creates an [AnnotatedString] from the passed String resource.
 *
 * @param stringResId The resource ID of the string to be converted to an [AnnotatedString].
 * @param onUrlClick Allows overriding the default behavior of URL clicks, by default it will open the URL in an
 * external browser.
 * @param args The arguments to be used in the string resource.
 */
@Composable
fun annotatedStringRes(
    @StringRes stringResId: Int,
    onUrlClick: ((String) -> Unit)? = null,
    vararg args: Any
): AnnotatedString {
    val linkInteractionListener = remember(onUrlClick) {
        if (onUrlClick == null) return@remember null

        LinkInteractionListener { linkAnnotation ->
            val url = when (linkAnnotation) {
                is LinkAnnotation.Url -> linkAnnotation.url
                is LinkAnnotation.Clickable -> linkAnnotation.tag
                else -> error("Unsupported LinkAnnotation type: $linkAnnotation")
            }
            onUrlClick.invoke(url)
        }
    }

    return AnnotatedString.fromHtml(
        stringResource(id = stringResId, *args),
        linkInteractionListener = linkInteractionListener,
        linkStyles = TextLinkStyles(
            style = SpanStyle(
                color = MaterialTheme.colors.primary,
                textDecoration = TextDecoration.None
            ),
        )
    )
}

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    return when (style) {
        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        else -> null
    }
}

fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is StyleSpan -> span.toSpanStyle()?.let {
                addStyle(style = it, start = start, end = end)
            }

            is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        }
    }
}
