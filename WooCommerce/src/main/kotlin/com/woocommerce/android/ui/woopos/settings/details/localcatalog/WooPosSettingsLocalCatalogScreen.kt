package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItemInfo

@Composable
fun WooPosSettingsLocalCatalogScreen(
    modifier: Modifier = Modifier,
    viewModel: WooPosSettingsLocalCatalogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    WooPosSettingsLocalCatalogScreen(
        state = state,
        onToggleCellularData = viewModel::toggleCellularDataUpdate,
        onRefreshCatalog = viewModel::runFullCatalogSync,
        modifier = modifier
    )
}

@Composable
private fun WooPosSettingsLocalCatalogScreen(
    modifier: Modifier = Modifier,
    state: WooPosSettingsLocalCatalogState,
    onToggleCellularData: (Boolean) -> Unit,
    onRefreshCatalog: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        CatalogStatusSection(
            catalogStatus = state.catalogStatus
        )

        CellularDataSection(
            allowCellularDataUpdate = state.allowCellularDataUpdate,
            onToggleCellularData = onToggleCellularData,
            isLoading = state.catalogStatus is WooPosSettingsLocalCatalogState.CatalogStatus.Loading ||
                state.catalogStatus is WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog
        )

        ManualUpdateSection(
            catalogStatus = state.catalogStatus,
            onRefreshCatalog = onRefreshCatalog,
        )
    }
}

@Composable
private fun CatalogStatusSection(
    catalogStatus: WooPosSettingsLocalCatalogState.CatalogStatus
) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(WooPosSpacing.Medium.value)
        ) {
            when (catalogStatus) {
                is WooPosSettingsLocalCatalogState.CatalogStatus.Available -> {
                    WooPosSettingsDetailsMenuItemInfo(
                        title = stringResource(R.string.woopos_settings_local_catalog_size),
                        subtitle = stringResource(
                            R.string.woopos_settings_local_catalog_size_format,
                            catalogStatus.productCount,
                            catalogStatus.variationCount
                        )
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                    WooPosSettingsDetailsMenuItemInfo(
                        title = stringResource(R.string.woopos_settings_local_catalog_last_update),
                        subtitle = catalogStatus.lastUpdate
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                    WooPosSettingsDetailsMenuItemInfo(
                        title = stringResource(R.string.woopos_settings_local_catalog_last_full_update),
                        subtitle = catalogStatus.lastFullUpdate
                    )
                }

                is WooPosSettingsLocalCatalogState.CatalogStatus.Loading,
                is WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog -> {
                    SettingLocalCatalogItemShimmer()

                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                    SettingLocalCatalogItemShimmer()

                    HorizontalDivider(modifier = Modifier.padding(vertical = WooPosSpacing.Medium.value))

                    SettingLocalCatalogItemShimmer()
                }
            }
        }
    }
}

@Composable
private fun CellularDataSection(
    allowCellularDataUpdate: Boolean,
    onToggleCellularData: (Boolean) -> Unit,
    isLoading: Boolean
) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) {
                    onToggleCellularData(!allowCellularDataUpdate)
                }
                .padding(WooPosSpacing.Medium.value)
        ) {
            val (title, subtitle, switch) = createRefs()

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_cellular_data),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(switch.start, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_cellular_data_subtitle),
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
                modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = WooPosSpacing.Small.value)
                    start.linkTo(parent.start)
                    end.linkTo(switch.start, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )

            Switch(
                checked = allowCellularDataUpdate,
                onCheckedChange = null,
                enabled = !isLoading,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.surfaceBright,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.constrainAs(switch) {
                    bottom.linkTo(subtitle.bottom)
                    top.linkTo(subtitle.top)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Composable
private fun ManualUpdateSection(
    catalogStatus: WooPosSettingsLocalCatalogState.CatalogStatus,
    onRefreshCatalog: () -> Unit
) {
    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value)
        ) {
            val (title, subtitle, button) = createRefs()

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_manual_update),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.constrainAs(title) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(button.start, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_refresh_description),
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
                modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = WooPosSpacing.Small.value)
                    start.linkTo(parent.start)
                    end.linkTo(button.start, margin = WooPosSpacing.Medium.value)
                    width = Dimension.fillToConstraints
                }
            )

            WooPosButtonSmall(
                text = stringResource(R.string.woopos_settings_local_catalog_refresh_button),
                onClick = onRefreshCatalog,
                state = when (catalogStatus) {
                    is WooPosSettingsLocalCatalogState.CatalogStatus.Available -> WooPosButtonState.ENABLED
                    WooPosSettingsLocalCatalogState.CatalogStatus.Loading -> WooPosButtonState.DISABLED
                    WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog -> WooPosButtonState.LOADING
                },
                modifier = Modifier.constrainAs(button) {
                    bottom.linkTo(subtitle.bottom)
                    top.linkTo(subtitle.top)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Composable
private fun SettingLocalCatalogItemShimmer() {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        WooPosShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.2f)
                .height(24.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.25f)
                .height(23.dp)
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
        )
    }
}

@Composable
fun WooPosSyncErrorDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_settings_local_catalog_sync_error_dialog_background_content_description
        ),
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceBright)
                .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
        ) {
            Row {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(
                            id = R.string.woopos_exit_dialog_confirmation_close_content_description
                        ),
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.size(WooPosSpacing.XLarge.value.toAdaptivePadding()))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    imageVector = WooPosIcons.ErrorX,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(WooPosSpacing.Medium.value.toAdaptivePadding())
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.Large.value.toAdaptivePadding()))

                WooPosText(
                    text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_title),
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

                WooPosText(
                    text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_message),
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))

                WooPosButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRetry,
                    text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_retry_button)
                )

                Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value.toAdaptivePadding()))

                WooPosOutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismissRequest,
                    text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_cancel_button)
                )
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsLocalCatalogScreenPreview() {
    WooPosTheme {
        WooPosSettingsLocalCatalogScreen(
            state = WooPosSettingsLocalCatalogState(
                catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.Available(
                    productCount = 1250,
                    variationCount = 3420,
                    lastUpdate = "2 hours ago",
                    lastFullUpdate = "Yesterday at 3:45 PM"
                ),
                allowCellularDataUpdate = true
            ),
            onToggleCellularData = {},
            onRefreshCatalog = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsLocalCatalogScreenLoadingPreview() {
    WooPosTheme {
        WooPosSettingsLocalCatalogScreen(
            state = WooPosSettingsLocalCatalogState(
                catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.Loading,
                allowCellularDataUpdate = true
            ),
            onToggleCellularData = {},
            onRefreshCatalog = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsLocalCatalogRefreshingPreview() {
    WooPosTheme {
        WooPosSettingsLocalCatalogScreen(
            state = WooPosSettingsLocalCatalogState(
                catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog,
                allowCellularDataUpdate = true
            ),
            onToggleCellularData = {},
            onRefreshCatalog = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSyncErrorDialogPreview() {
    WooPosTheme {
        WooPosSyncErrorDialog(
            isVisible = true,
            onRetry = {},
            onDismissRequest = {}
        )
    }
}
