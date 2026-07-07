package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageType
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItem
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItemSkeleton

@Composable
fun WooShippingSavedPackageScreen(
    viewModel: WooShippingLabelPackageCreationViewModel,
    onTabChange: (PageType) -> Unit
) {
    val viewState = viewModel.viewState.observeAsState()
    WooShippingSavedPackageScreen(
        packageState = viewState.value?.packagesState ?: PackagesState.Waiting,
        isAddPackageEnabled = viewState.value?.packagesData?.hasSavedSelection == true,
        onAddPackageClick = viewModel::onAddSavedPackageClick,
        onSavedPackageSelected = viewModel::onSavedPackageSelected,
        onSavedPackageRemoved = viewModel::onSavedPackageRemoved,
        onRetryClick = viewModel::onRetryClick,
        onTabChange = onTabChange
    )
}

@Composable
fun WooShippingSavedPackageScreen(
    modifier: Modifier = Modifier,
    packageState: PackagesState,
    isAddPackageEnabled: Boolean,
    onAddPackageClick: () -> Unit,
    onSavedPackageSelected: (PackageData, Boolean) -> Unit,
    onSavedPackageRemoved: (PackageData) -> Unit,
    onRetryClick: () -> Unit,
    onTabChange: (PageType) -> Unit
) {
    Column(modifier = modifier) {
        Box(modifier = modifier.weight(1f)) {
            when {
                packageState is PackagesState.Data && packageState.savedPackages.isEmpty() -> EmptyPackages(
                    modifier = modifier,
                    R.drawable.ic_boxes,
                    R.string.woo_shipping_labels_package_creation_empty_saved_message
                ) { onTabChange(PageType.CUSTOM) }

                packageState is PackagesState.Data -> {
                    WooShippingSavedPackageContent(
                        modifier = modifier,
                        savedPackages = packageState.savedPackages,
                        onSavedPackageSelected = onSavedPackageSelected,
                        onSavedPackageRemoved = onSavedPackageRemoved
                    )
                }

                packageState is PackagesState.Error -> {
                    ErrorMessageWithButton(
                        modifier = modifier,
                        message = R.string.woo_shipping_labels_package_creation_saved_loading_error,
                        onRetryClick = onRetryClick
                    )
                }

                packageState is PackagesState.Waiting -> {
                    Column(
                        modifier = modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        WooShippingPackageListItemSkeleton()
                        WooShippingPackageListItemSkeleton()
                        WooShippingPackageListItemSkeleton()
                    }
                }
            }
        }
        Divider()
        WCColoredButton(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = isAddPackageEnabled,
            onClick = onAddPackageClick
        ) {
            Text(
                stringResource(
                    id = if (isAddPackageEnabled) {
                        R.string.woo_shipping_labels_package_creation_add_package
                    } else {
                        R.string.shipping_label_select_package_button
                    }
                )
            )
        }
    }
}

@Composable
fun WooShippingSavedPackageContent(
    modifier: Modifier = Modifier,
    savedPackages: List<PackageData>,
    onSavedPackageSelected: (PackageData, Boolean) -> Unit,
    onSavedPackageRemoved: (PackageData) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(
            items = savedPackages,
            key = { index, packageData -> packageData.id }
        ) { index, packageData ->
            WooShippingPackageListItem(
                modifier = modifier.animateItem(),
                packageData = packageData,
                onPackageSelected = onSavedPackageSelected,
                divider = index < savedPackages.size - 1,
                packageItemSupportsStarring = false,
                onPackageRemoved = onSavedPackageRemoved
            )
        }
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PackagesState.Data(
                storeOptions = StoreOptionsForPackages.DEFAULT,
                savedPackages = listOf(
                    PackageData(
                        name = "Small Flat Rate Box",
                        dimensions = "10 x 10 x 10 cm",
                        weight = "10",
                        isSelected = true,
                        isLetter = true,
                        id = "1",
                    ),
                    PackageData(
                        name = "Small Flat Rate Box",
                        dimensions = "20 x 20 x 20 cm",
                        weight = "20",
                        isSelected = false,
                        isLetter = false,
                        id = "1",
                    ),
                    PackageData(
                        name = "Small Flat Rate Box",
                        dimensions = "30 x 30 x 30 cm",
                        weight = "30",
                        isSelected = false,
                        isLetter = false,
                        id = "1",
                    )
                ),
                carrierPackages = emptyMap()
            ),
            isAddPackageEnabled = true,
            onAddPackageClick = {},
            onSavedPackageSelected = { _, _ -> },
            onSavedPackageRemoved = { _ -> },
            onRetryClick = {},
            onTabChange = {}
        )
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenLoadingPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PackagesState.Waiting,
            isAddPackageEnabled = false,
            onAddPackageClick = {},
            onSavedPackageSelected = { _, _ -> },
            onSavedPackageRemoved = { _ -> },
            onRetryClick = {},
            onTabChange = {}
        )
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenErrorPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PackagesState.Error(),
            isAddPackageEnabled = false,
            onAddPackageClick = {},
            onSavedPackageSelected = { _, _ -> },
            onSavedPackageRemoved = { _ -> },
            onRetryClick = {},
            onTabChange = {}
        )
    }
}
