package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme

@Composable
fun WooPosCatalogSyncOverdueBanner(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    WooPosItemsInfoBanner(
        visible = visible,
        title = stringResource(R.string.woopos_refresh_catalog_banner_title),
        message = stringResource(R.string.woopos_refresh_catalog_banner_message),
        dismissContentDescription = stringResource(R.string.woopos_refresh_catalog_banner_dismiss),
        onDismiss = onDismiss
    )
}

@Composable
@WooPosPreview
fun WooPosRefreshCatalogBannerPreview() {
    WooPosTheme {
        WooPosCatalogSyncOverdueBanner(
            visible = true,
            onDismiss = {}
        )
    }
}
