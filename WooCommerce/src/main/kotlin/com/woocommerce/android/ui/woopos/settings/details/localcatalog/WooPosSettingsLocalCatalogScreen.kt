package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
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

        if (state.cellularCapability is WooPosSettingsLocalCatalogState.CellularCapability.Available) {
            CellularDataSection(
                allowCellularDataUpdate = state.cellularCapability.allowCellularDataUpdate,
                onToggleCellularData = onToggleCellularData,
                isLoading = state.catalogStatus is WooPosSettingsLocalCatalogState.CatalogStatus.Loading ||
                    state.catalogStatus is WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog
            )
        }

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
    val marginMedium = WooPosSpacing.Medium.value
    val marginSmall = WooPosSpacing.Small.value

    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading) {
                    onToggleCellularData(!allowCellularDataUpdate)
                }
                .padding(marginMedium)
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
                    end.linkTo(switch.start, margin = marginMedium)
                    width = Dimension.fillToConstraints
                }
            )

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_cellular_data_subtitle),
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = marginSmall)
                    start.linkTo(parent.start)
                    end.linkTo(switch.start, margin = marginMedium)
                    width = Dimension.fillToConstraints
                }
            )

            Switch(
                checked = allowCellularDataUpdate,
                onCheckedChange = null,
                enabled = !isLoading,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFFFFFFF),
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.constrainAs(switch) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
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
    val marginMedium = WooPosSpacing.Medium.value
    val marginSmall = WooPosSpacing.Small.value

    WooPosCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(marginMedium)
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
                    end.linkTo(button.start, margin = marginMedium)
                    width = Dimension.fillToConstraints
                }
            )

            WooPosText(
                text = stringResource(R.string.woopos_settings_local_catalog_refresh_description),
                style = WooPosTypography.BodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.constrainAs(subtitle) {
                    top.linkTo(title.bottom, margin = marginSmall)
                    start.linkTo(parent.start)
                    end.linkTo(button.start, margin = marginMedium)
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
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
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
        WooPosShimmerText(
            text = stringResource(R.string.woopos_settings_local_catalog_last_full_update),
            style = WooPosTypography.BodyMedium.style
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.XSmall.value))

        WooPosShimmerText(
            text = "Yesterday at 3:45 PM",
            style = WooPosTypography.BodyMedium.style
        )
    }
}

@Composable
fun WooPosSyncErrorDialog(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isServerPermissionsError: Boolean = false,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            id = R.string.woopos_settings_local_catalog_sync_error_dialog_background_content_description
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest
    ) {
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
                    .padding(bottom = WooPosSpacing.Medium.value)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

            WooPosText(
                text = stringResource(
                    if (isServerPermissionsError) {
                        R.string.woopos_settings_local_catalog_sync_error_blocked_title
                    } else {
                        R.string.woopos_settings_local_catalog_sync_error_dialog_title
                    }
                ),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = stringResource(
                    if (isServerPermissionsError) {
                        R.string.woopos_settings_local_catalog_sync_error_blocked_message
                    } else {
                        R.string.woopos_settings_local_catalog_sync_error_dialog_message
                    }
                ),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value))

            WooPosButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onRetry,
                text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_retry_button)
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDismissRequest,
                text = stringResource(R.string.woopos_settings_local_catalog_sync_error_dialog_cancel_button)
            )
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
                cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.Available(
                    allowCellularDataUpdate = true
                )
            ),
            onToggleCellularData = {},
            onRefreshCatalog = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsLocalCatalogScreenNoCellularPreview() {
    WooPosTheme {
        WooPosSettingsLocalCatalogScreen(
            state = WooPosSettingsLocalCatalogState(
                catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.Available(
                    productCount = 1250,
                    variationCount = 3420,
                    lastUpdate = "2 hours ago",
                    lastFullUpdate = "Yesterday at 3:45 PM"
                ),
                cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.None
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
                cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.Available(
                    allowCellularDataUpdate = true
                )
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
                cellularCapability = WooPosSettingsLocalCatalogState.CellularCapability.Available(
                    allowCellularDataUpdate = true
                )
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
