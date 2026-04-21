package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderTipStrip(
    onExplainerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tipText = stringResource(R.string.woopos_remote_ttp_tip_strip_text)
    val learnHowText = stringResource(R.string.woopos_remote_ttp_tip_strip_learn_how)
    val linkColor = MaterialTheme.colorScheme.primary

    val annotatedText = buildAnnotatedString {
        val learnHowStart = tipText.indexOf(learnHowText)
        if (learnHowStart >= 0) {
            append(tipText.substring(0, learnHowStart))
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                )
            ) {
                append(learnHowText)
            }
            append(tipText.substring(learnHowStart + learnHowText.length))
        } else {
            append(tipText)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
            .background(MaterialTheme.colorScheme.surfaceDim)
            .clickable { onExplainerClick() }
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Small.value,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosText(
            text = annotatedText,
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderTipStripPreview() {
    WooPosTheme {
        WooPosRemoteReaderTipStrip(onExplainerClick = {})
    }
}
