package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosRefreshCatalogBanner(
    bannerState: WooPosItemsViewModel.CatalogSyncBannerState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = bannerState is WooPosItemsViewModel.CatalogSyncBannerState.OverdueWarning,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 180)
        ) + scaleIn(
            animationSpec = tween(durationMillis = 180)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
                )
                .padding(
                    vertical = WooPosSpacing.Small.value,
                    horizontal = WooPosSpacing.Medium.value
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                contentDescription = null
            )
            Spacer(Modifier.size(WooPosSpacing.Small.value))
            WooPosText(
                text = stringResource(R.string.woopos_refresh_catalog_banner_message),
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
@WooPosPreview
fun WooPosRefreshCatalogBannerPreview() {
    WooPosTheme {
        WooPosRefreshCatalogBanner(
            bannerState = WooPosItemsViewModel.CatalogSyncBannerState.OverdueWarning
        )
    }
}
