@file:Suppress("WooPosDesignSystemTextUsageRule")

package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: WooPosTypography,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.style,
        fontWeight = fontWeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color,
        textDecoration = textDecoration,
        textAlign = textAlign
    )
}

@Composable
fun WooPosText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: WooPosTypography,
    fontWeight: FontWeight? = null,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.style,
        fontWeight = fontWeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        color = color,
        textDecoration = textDecoration,
        textAlign = textAlign
    )
}
