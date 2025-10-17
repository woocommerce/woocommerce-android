package com.woocommerce.android.ui.woopos.home.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosCatalogSyncOverdueBanner(
    state: WooPosItemsViewModel.CatalogSyncOverdueBannerState,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = state is WooPosItemsViewModel.CatalogSyncOverdueBannerState.Visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 180)
        ) + scaleIn(
            animationSpec = tween(durationMillis = 180)
        ),
        modifier = modifier.padding(horizontal = WooPosSpacing.Small.value)
    ) {
        WooPosCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
            elevation = WooPosElevation.Medium,
            shadowType = ShadowType.Soft,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WooPosSpacing.Medium.value),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_woo_pos_info_banner),
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterVertically)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                ) {
                    WooPosText(
                        text = stringResource(R.string.woopos_refresh_catalog_banner_title),
                        style = WooPosTypography.BodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    WooPosText(
                        text = stringResource(R.string.woopos_refresh_catalog_banner_message),
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.woopos_refresh_catalog_banner_dismiss),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosRefreshCatalogBannerPreview() {
    WooPosTheme {
        WooPosCatalogSyncOverdueBanner(
            state = WooPosItemsViewModel.CatalogSyncOverdueBannerState.Visible,
            onDismiss = {}
        )
    }
}
