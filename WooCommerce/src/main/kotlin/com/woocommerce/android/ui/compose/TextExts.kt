package com.woocommerce.android.ui.compose

import android.graphics.Typeface
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import androidx.annotation.StringRes
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.text.HtmlCompat

const val URL_ANNOTATION_TAG = "url"

/**
 * Creates an [AnnotatedString] from the passed String resource.
 *
 * Source: https://stackoverflow.com/a/70757314 with some adjustments
 */
@Composable
fun annotatedStringRes(@StringRes stringResId: Int, vararg args: Any): AnnotatedString {
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
                        style = TextStyle.Default.copy(color = MaterialTheme.colors.secondary).toSpanStyle(),
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

private fun StyleSpan.toSpanStyle(): SpanStyle? {
    return when (style) {
        Typeface.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
        Typeface.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
        Typeface.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        else -> null
    }
}
