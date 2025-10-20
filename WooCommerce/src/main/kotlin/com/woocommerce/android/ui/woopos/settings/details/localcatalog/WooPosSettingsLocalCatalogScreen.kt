package com.woocommerce.android.ui.woopos.settings.details.localcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

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
    state: WooPosSettingsLocalCatalogState,
    onToggleCellularData: (Boolean) -> Unit,
    onRefreshCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(WooPosSpacing.Medium.value)
    ) {
        CatalogStatusSection(
            catalogStatus = state.catalogStatus
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        SettingsSection(
            allowCellularDataUpdate = state.allowCellularDataUpdate,
            onToggleCellularData = onToggleCellularData,
            isLoading = state.catalogStatus is WooPosSettingsLocalCatalogState.CatalogStatus.LoadingStatus
        )

        Spacer(modifier = Modifier.height(WooPosSpacing.Large.value))

        RefreshSection(
            onRefreshCatalog = onRefreshCatalog,
            catalogStatus = state.catalogStatus
        )
    }
}

@Composable
private fun CatalogStatusSection(
    catalogStatus: WooPosSettingsLocalCatalogState.CatalogStatus
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Large.value))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(WooPosSpacing.Medium.value)
    ) {
        SectionTitle(stringResource(R.string.woopos_settings_local_catalog_status))

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        when (catalogStatus) {
            is WooPosSettingsLocalCatalogState.CatalogStatus.Available -> {
                CatalogSizeRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_size),
                    productCount = catalogStatus.productCount,
                    variationCount = catalogStatus.variationCount,
                    isLoading = false
                )
                StatusRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_last_update),
                    value = catalogStatus.lastUpdate,
                    isLoading = false
                )
                StatusRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_last_full_update),
                    value = catalogStatus.lastFullUpdate,
                    isLoading = false
                )
            }
            is WooPosSettingsLocalCatalogState.CatalogStatus.LoadingStatus,
            is WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog -> {
                CatalogSizeRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_size),
                    productCount = 0,
                    variationCount = 0,
                    isLoading = true
                )
                StatusRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_last_update),
                    value = null,
                    isLoading = true
                )
                StatusRow(
                    label = stringResource(R.string.woopos_settings_local_catalog_last_full_update),
                    value = null,
                    isLoading = true
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    allowCellularDataUpdate: Boolean,
    onToggleCellularData: (Boolean) -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Large.value))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(WooPosSpacing.Medium.value)
    ) {
        SectionTitle(stringResource(R.string.woopos_settings_local_catalog_settings))

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                WooPosText(
                    text = stringResource(R.string.woopos_settings_local_catalog_cellular_data),
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WooPosText(
                    text = stringResource(R.string.woopos_settings_local_catalog_cellular_data_subtitle),
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
                )
            }

            Switch(
                checked = allowCellularDataUpdate,
                onCheckedChange = { onToggleCellularData(it) },
                enabled = !isLoading,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun RefreshSection(
    onRefreshCatalog: () -> Unit,
    catalogStatus: WooPosSettingsLocalCatalogState.CatalogStatus
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(WooPosCornerRadius.Large.value))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(WooPosSpacing.Medium.value)
    ) {
        SectionTitle(stringResource(R.string.woopos_settings_local_catalog_actions))

        Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

        WooPosText(
            text = stringResource(R.string.woopos_settings_local_catalog_refresh_description),
            style = WooPosTypography.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value)
        )

        WooPosButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRefreshCatalog,
            state = when (catalogStatus) {
                is WooPosSettingsLocalCatalogState.CatalogStatus.Available -> WooPosButtonState.ENABLED
                WooPosSettingsLocalCatalogState.CatalogStatus.LoadingStatus -> WooPosButtonState.DISABLED
                WooPosSettingsLocalCatalogState.CatalogStatus.RefreshingCatalog -> WooPosButtonState.LOADING
            },
            text = stringResource(R.string.woopos_settings_local_catalog_refresh_button)
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String?,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading) {
            WooPosShimmerBox(
                modifier = Modifier
                    .width(100.dp)
                    .height(20.dp),
            )
        } else {
            WooPosText(
                text = value ?: "-",
                style = WooPosTypography.BodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CatalogSizeRow(
    label: String,
    productCount: Int,
    variationCount: Int,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        WooPosText(
            text = label,
            style = WooPosTypography.BodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading) {
            WooPosShimmerBox(
                modifier = Modifier
                    .width(100.dp)
                    .height(20.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                WooPosText(
                    text = "$productCount products",
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                WooPosText(
                    text = "$variationCount variations",
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    WooPosText(
        text = title,
        style = WooPosTypography.BodyXLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
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
                catalogStatus = WooPosSettingsLocalCatalogState.CatalogStatus.LoadingStatus,
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
