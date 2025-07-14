package com.woocommerce.android.ui.compose.component

import android.text.style.URLSpan
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun LearnMoreAboutSection(
    modifier: Modifier = Modifier,
    @StringRes textWithUrl: Int,
    onClick: () -> Unit
) {
    val textValue = stringResource(textWithUrl)
    val textWithSpans = remember(textWithUrl) {
        HtmlCompat.fromHtml(
            textValue,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
    val spans = textWithSpans.getSpans(0, textWithSpans.length, URLSpan::class.java)
    val (urlStart, urlEnd) = spans.let {
        if (it.isEmpty()) {
            0 to 0
        } else {
            textWithSpans.getSpanStart(it.firstOrNull()) to textWithSpans.getSpanEnd(it.firstOrNull())
        }
    }
    LearnMoreAboutSection(
        modifier = modifier,
        text = TextWithHighlighting(
            text = textWithSpans.toString(),
            start = urlStart,
            end = urlEnd,
        ),
        onClick = onClick
    )
}

@Composable
private fun LearnMoreAboutSection(
    modifier: Modifier = Modifier,
    text: TextWithHighlighting,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Icon(
            painter = painterResource(id = R.drawable.ic_info_outline_20dp),
            contentDescription = text.toString(),
            tint = colorResource(id = R.color.color_icon)
        )
        Spacer(modifier = Modifier.width(16.dp))

        val colorHigh = colorResource(id = R.color.color_on_surface_high)
        val colorPrimary = colorResource(id = R.color.color_primary)

        val annotatedText = remember(text) {
            buildAnnotatedString {
                val clickableStyle = SpanStyle(
                    color = colorHigh,
                    textDecoration = TextDecoration.Underline
                )

                withStyle(style = clickableStyle) {
                    withStyle(style = SpanStyle(color = colorPrimary)) {
                        append(text.text.substring(text.start, text.end))
                    }
                }
                append(text.text.substring(text.end))
            }
        }

        Text(
            text = annotatedText,
            modifier = Modifier.fillMaxWidth(),
            color = colorHigh,
            style = MaterialTheme.typography.caption,
        )
        Spacer(modifier = Modifier.width(16.dp))
    }
}

private data class TextWithHighlighting(val text: String, val start: Int, val end: Int)

@LightDarkThemePreviews
@Composable
fun LearnMoreComponentPreviewWithHtml() {
    LearnMoreAboutSection(
        textWithUrl = R.string.card_reader_detail_learn_more,
        onClick = {}
    )
}

@LightDarkThemePreviews
@Composable
fun LearnMoreComponentPreviewWithoutHtml() {
    LearnMoreAboutSection(
        textWithUrl = R.string.card_reader_connect_missing_bluetooth_permissions_header,
        onClick = {}
    )
}
