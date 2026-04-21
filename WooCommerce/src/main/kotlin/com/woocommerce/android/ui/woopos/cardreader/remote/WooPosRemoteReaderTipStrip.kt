package com.woocommerce.android.ui.woopos.cardreader.remote

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRemoteReaderTipStrip(modifier: Modifier = Modifier) {
    WooPosText(
        text = stringResource(R.string.woopos_remote_ttp_tip_strip_text),
        style = WooPosTypography.BodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = WooPosSpacing.Large.value,
                vertical = WooPosSpacing.Medium.value,
            ),
    )
}

@WooPosPreview
@Composable
fun WooPosRemoteReaderTipStripPreview() {
    WooPosTheme {
        WooPosRemoteReaderTipStrip()
    }
}
