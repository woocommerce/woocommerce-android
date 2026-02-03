package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

@Composable
fun WooPosWooCommerceVersionSunsetBanner(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    WooPosItemsInfoBanner(
        visible = visible,
        title = stringResource(R.string.woopos_wc_version_sunset_banner_title),
        message = stringResource(R.string.woopos_wc_version_sunset_banner_message),
        dismissContentDescription = stringResource(R.string.woopos_wc_version_sunset_banner_dismiss),
        onDismiss = onDismiss
    )
}

@Composable
@WooPosPreview
fun WooPosWooCommerceVersionSunsetBannerPreview() {
    WooPosTheme {
        WooPosWooCommerceVersionSunsetBanner(
            visible = true,
            onDismiss = {}
        )
    }
}
