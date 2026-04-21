package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderTipStrip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosText(
        text = stringResource(R.string.woopos_remote_ttp_tip_strip_text),
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = WooPosSpacing.Large.value,
                vertical = WooPosSpacing.Small.value,
            ),
    )
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderTipStripPreview() {
    WooPosTheme {
        WooPosRemoteReaderTipStrip(onClick = {})
    }
}
