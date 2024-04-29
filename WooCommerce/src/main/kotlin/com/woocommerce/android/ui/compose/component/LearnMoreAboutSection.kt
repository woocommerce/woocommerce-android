package com.woocommerce.android.ui.compose.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews

@Composable
fun LearnMoreAboutSection(
    text: TextWithHighlighting,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_info_outline_20dp),
            contentDescription = text.toString(),
            tint = colorResource(id = R.color.color_icon)
        )
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = buildAnnotatedString {
                val clickableStyle = SpanStyle(
                    color = colorResource(id = R.color.color_on_surface_high),
                    textDecoration = TextDecoration.Underline
                )

                withStyle(style = clickableStyle) {
                    withStyle(style = SpanStyle(color = colorResource(id = R.color.color_primary))) {
                        append(text.text.substring(text.start, text.end))
                    }
                }
                append(text.text.substring(text.end))
            },
            modifier = Modifier.fillMaxWidth(),
            color = colorResource(id = R.color.color_on_surface_high),
            style = MaterialTheme.typography.caption,
        )
    }
}

data class TextWithHighlighting(val text: String, val start: Int, val end: Int)

@LightDarkThemePreviews
@Composable
fun LearnMoreComponentPreview() {
    LearnMoreAboutSection(
        text = TextWithHighlighting("Learn more about Something", 0, 10),
        onClick = {}
    )
}
