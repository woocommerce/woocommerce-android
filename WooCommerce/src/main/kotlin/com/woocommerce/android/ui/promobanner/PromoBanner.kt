package com.woocommerce.android.ui.promobanner

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.payments.banner.Banner

@Composable
fun PromoBanner(
    bannerType: PromoBannerType,
    onCtaClick: (String) -> Unit,
    onDismissClick: () -> Unit
) {
    Banner(
        onCtaClick = onCtaClick,
        onDismissClick = onDismissClick,
        title = stringResource(id = bannerType.titleRes),
        subtitle = stringResource(id = bannerType.messageRes),
        ctaLabel = stringResource(R.string.set_up_now),
        source = "",
        chipLabel = stringResource(id = R.string.tip)
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
