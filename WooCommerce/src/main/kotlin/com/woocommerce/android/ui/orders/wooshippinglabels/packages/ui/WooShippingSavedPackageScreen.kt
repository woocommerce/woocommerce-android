package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PredefinedPackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItem
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItemSkeleton

@Composable
fun WooShippingSavedPackageScreen(viewModel: WooShippingLabelPackageCreationViewModel) {
    val viewState = viewModel.viewState.observeAsState()
    WooShippingSavedPackageScreen(
        packageState = viewState.value?.predefinedPackagesState ?: PredefinedPackagesState.Waiting,
        isAddPackageEnabled = viewState.value?.predefinedPackagesData?.hasSavedSelection ?: false,
        onAddPackageClick = viewModel::onAddSavedPackageClick,
        onSavedPackageSelected = viewModel::onSavedPackageSelected,
        onRetryClick = viewModel::onRetryClick
    )
}

@Composable
fun WooShippingSavedPackageScreen(
    modifier: Modifier = Modifier,
    packageState: PredefinedPackagesState,
    isAddPackageEnabled: Boolean,
    onAddPackageClick: () -> Unit,
    onSavedPackageSelected: (PackageData, Boolean) -> Unit,
    onRetryClick: () -> Unit
) {
    Column(modifier = modifier) {
        Box(modifier = modifier.weight(1f)) {
            when (packageState) {
                is PredefinedPackagesState.Data -> {
                    WooShippingSavedPackageContent(
                        modifier = modifier,
                        savedPackages = packageState.savedPackages,
                        onSavedPackageSelected = onSavedPackageSelected
                    )
                }

                is PredefinedPackagesState.Error -> {
                    ErrorMessageWithButton(
                        modifier = modifier,
                        message = R.string.woo_shipping_labels_package_creation_saved_loading_error,
                        onRetryClick = onRetryClick
                    )
                }

                is PredefinedPackagesState.Waiting -> {
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
        Button(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            enabled = isAddPackageEnabled,
            onClick = onAddPackageClick
        ) {
            Text(stringResource(id = R.string.woo_shipping_labels_package_creation_add_package))
        }
    }
}

@Composable
fun WooShippingSavedPackageContent(
    modifier: Modifier = Modifier,
    savedPackages: List<PackageData>,
    onSavedPackageSelected: (PackageData, Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        savedPackages.forEach { packageData ->
            WooShippingPackageListItem(
                modifier,
                packageData,
                onSavedPackageSelected
            )
        }
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PredefinedPackagesState.Data(
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
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenLoadingPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PredefinedPackagesState.Waiting,
            isAddPackageEnabled = false,
            onAddPackageClick = {},
            onSavedPackageSelected = { _, _ -> },
            onRetryClick = {}
        )
    }
}

@Preview
@Composable
fun WooShippingSavedPackageScreenErrorPreview() {
    WooThemeWithBackground {
        WooShippingSavedPackageScreen(
            packageState = PredefinedPackagesState.Error,
            isAddPackageEnabled = false,
            onAddPackageClick = {},
            onSavedPackageSelected = { _, _ -> },
            onRetryClick = {}
        )
    }
}
