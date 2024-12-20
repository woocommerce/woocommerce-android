package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
            onSiteSelected = viewModel::onSiteSelected,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun WooSitesVisibilityScreen(
    state: WooStoresUiState,
    onBack: () -> Unit,
    onSaveTapped: () -> Unit,
    onSiteSelected: (WooStoreUi) -> Unit,
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
                TextButton(
                    onClick = onSaveTapped,
                    enabled = state.isSaveButtonEnabled
                ) {
                    Text(
                        text = stringResource(id = R.string.save).uppercase()
                    )
                }
            },
            elevation = 0.dp
        )
    }) { padding ->
        Column {
            LazyColumn(
                modifier = modifier
                    .padding(padding)
                    .background(MaterialTheme.colors.surface)
                    .padding(horizontal = 16.dp)
            ) {
                itemsIndexed(state.wooStores) { index, wooStore ->
                    Row(
                        modifier = Modifier.clickable { onSiteSelected(wooStore) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SelectionCheck(
                            isSelected = wooStore.isSelected,
                            onSelectionChange = null,
                        )
                        Column(
                            Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        ) {
                            Text(
                                text = wooStore.siteName,
                                style = WooTypography.body1,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = wooStore.siteUrl,
                                style = WooTypography.body2,
                                color = colorResource(id = R.color.color_on_surface_medium)
                            )
                            Spacer(Modifier.height(16.dp))
                            if (index < state.wooStores.size - 1) {
                                Divider()
                            }
                        }
                    }
                }
            }
            Text(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.site_picker_edit_store_list_footer),
                style = WooTypography.caption,
                color = MaterialTheme.colors.onSurface,
            )
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
                    siteName = "My Store",
                    siteUrl = "https://example.com",
                    siteId = 1,
                    isSelected = true
                )
            ),
            isSaveButtonEnabled = true
        ),
        onBack = {},
        onSaveTapped = {},
        onSiteSelected = {}
    )
}
