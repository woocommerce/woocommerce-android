package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.SelectionCheck
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooTypography
import com.woocommerce.android.ui.sitepicker.sitevisibility.WooSitesVisibilityViewModel.WooStoreUi
import com.woocommerce.android.ui.sitepicker.sitevisibility.WooSitesVisibilityViewModel.WooStoresUiState

@Composable
fun WooSitesVisibilityScreen(viewModel: WooSitesVisibilityViewModel) {
    BackHandler(onBack = viewModel::onBackPressed)
    viewModel.viewState.observeAsState().value?.let { state ->
        WooSitesVisibilityScreen(
            state = state,
            onBack = viewModel::onBackPressed,
            onSaveTapped = viewModel::onSaveTapped,
            onSiteTapped = viewModel::onSiteTapped,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WooSitesVisibilityScreen(
    state: WooStoresUiState,
    onBack: () -> Unit,
    onSaveTapped: () -> Unit,
    onSiteTapped: (WooStoreUi) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(text = stringResource(id = R.string.site_picker_edit_store_list_title)) },
            navigationIcon = {
                IconButton(onBack) {
                    Icon(
                        Filled.Close,
                        contentDescription = stringResource(id = R.string.back)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            actions = {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(width = 56.dp, height = 16.dp)
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    TextButton(
                        onClick = onSaveTapped,
                        enabled = state.isSaveButtonEnabled
                    ) {
                        Text(
                            text = stringResource(id = R.string.save).uppercase()
                        )
                    }
                }
            },
            elevation = 0.dp
        )
    }) { padding ->
        val borderWidth = 1.dp
        val borderColor = colorResource(id = R.color.divider_color)
        Column(
            modifier = modifier
                .padding(padding)
                .background(MaterialTheme.colors.surface)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                text = stringResource(R.string.site_picker_edit_store_current_site_header),
                style = WooTypography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(R.string.site_picker_edit_store_current_site_footer),
                style = WooTypography.caption,
                color = MaterialTheme.colors.onSurface,
            )
            StoreItem(
                wooStore = state.currentSite,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            )
            Text(
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
                text = stringResource(R.string.site_picker_edit_store_list_header),
                style = WooTypography.subtitle1,
                color = MaterialTheme.colors.onSurface,
            )
            Text(
                modifier = Modifier.padding(bottom = 8.dp),
                text = stringResource(R.string.site_picker_edit_store_list_footer),
                style = WooTypography.caption,
                color = MaterialTheme.colors.onSurface,
            )
            AvailableStoresForHiding(
                state = state,
                onSiteSelected = onSiteTapped,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    )
            )
        }
    }
}

@Composable
private fun AvailableStoresForHiding(
    state: WooStoresUiState,
    onSiteSelected: (WooStoreUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        state.wooStores.forEachIndexed { index, wooStore ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSiteSelected(wooStore) }
                    .padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SelectionCheck(
                    isSelected = wooStore.isSelected,
                    onSelectionChange = null,
                )
                StoreItem(
                    wooStore = wooStore,
                    showDivider = index < state.wooStores.size - 1,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun StoreItem(
    wooStore: WooStoreUi,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = wooStore.siteName,
            style = WooTypography.body1,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.padding(end = 8.dp),
            text = wooStore.siteUrl,
            style = WooTypography.body2,
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        Spacer(Modifier.height(16.dp))
        if (showDivider) {
            Divider()
        }
    }
}

@LightDarkThemePreviews
@Composable
fun StoreVisibilityScreenPreview() {
    WooSitesVisibilityScreen(
        state = WooStoresUiState(
            wooStores = listOf(
                WooStoreUi(
                    siteName = "Another Store",
                    siteUrl = "https://example.com",
                    siteId = 1,
                    isSelected = true
                ),
                WooStoreUi(
                    siteName = "Any Store",
                    siteUrl = "https://example.com",
                    siteId = 1,
                    isSelected = true
                ),
                WooStoreUi(
                    siteName = "Some Store",
                    siteUrl = "https://example.com",
                    siteId = 1,
                    isSelected = true
                )

            ),
            isSaveButtonEnabled = true,
            isLoading = false,
            currentSite = WooStoreUi(
                siteName = "Current Store",
                siteUrl = "https://myselectedSite.com",
                siteId = 1,
                isSelected = true
            )
        ),
        onBack = {},
        onSaveTapped = {},
        onSiteTapped = {}
    )
}
