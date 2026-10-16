package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBanner
import com.woocommerce.android.ui.compose.designsystem.component.WooNoticeBannerTone
import com.woocommerce.android.ui.compose.designsystem.foundation.WooDesignSystemTheme

@Composable
internal fun WooShippingCustomsPluginUpdateNotice(modifier: Modifier = Modifier) {
    WooDesignSystemTheme {
        WooNoticeBanner(
            title = stringResource(R.string.woo_shipping_labels_customs_eu_product_identifiers_plugin_update_notice),
            tone = WooNoticeBannerTone.Info,
            modifier = modifier
        )
    }
}

@PreviewLightDark
@Composable
private fun WooShippingCustomsPluginUpdateNoticePreview() {
    WooShippingCustomsPluginUpdateNotice()
}
