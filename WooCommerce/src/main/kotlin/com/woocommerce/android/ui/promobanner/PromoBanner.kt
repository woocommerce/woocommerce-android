package com.woocommerce.android.ui.promobanner

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.banner.Banner
import com.woocommerce.android.ui.payments.banner.BannerState

@Composable
fun PromoBanner(
    bannerType: PromoBannerType,
    onCtaClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Banner(
        BannerState(
            shouldDisplayBanner = true,
            onPrimaryActionClicked = onCtaClick,
            onDismissClicked = onDismissClick,
            title = UiString.UiStringRes(bannerType.titleRes),
            description = UiString.UiStringRes(bannerType.messageRes),
            primaryActionLabel = UiString.UiStringRes(R.string.set_up_now),
            chipLabel = UiString.UiStringRes(R.string.tip)
        )
    )
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PromoBannerPreview() {
    WooThemeWithBackground {
        PromoBanner(PromoBannerType.LINKED_PRODUCTS, {}, {})
    }
}
