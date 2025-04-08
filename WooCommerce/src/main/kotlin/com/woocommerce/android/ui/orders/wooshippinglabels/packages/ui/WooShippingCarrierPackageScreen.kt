package com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LeadingIconTab
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageType
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PredefinedPackagesState
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.ErrorMessageWithButton
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItem
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.components.WooShippingPackageListItemSkeleton
import kotlinx.coroutines.launch

@Composable
fun WooShippingCarrierPackageScreen(
    viewModel: WooShippingLabelPackageCreationViewModel,
    onTabChange: (PageType) -> Unit
) {
    val viewState by viewModel.viewState.observeAsState()
    WooShippingCarrierPackageScreen(
        packageState = viewState?.predefinedPackagesState ?: PredefinedPackagesState.Waiting,
        isAddPackageEnabled = viewState?.predefinedPackagesData?.hasCarrierSelection == true,
        onPackageSelected = viewModel::onCarrierPackageSelected,
        onAddPackageClick = viewModel::onAddCarrierPackageClick,
        onRetryClick = viewModel::onRetryClick,
        onTabChange = onTabChange
    )
}

@Composable
fun WooShippingCarrierPackageScreen(
    modifier: Modifier = Modifier,
    packageState: PredefinedPackagesState,
    onPackageSelected: (PackageData, Boolean) -> Unit,
    isAddPackageEnabled: Boolean = false,
    onAddPackageClick: () -> Unit = {},
    onRetryClick: () -> Unit,
    onTabChange: (PageType) -> Unit
) {
    Column(modifier = modifier) {
        Box(modifier = modifier.weight(1f)) {
            when {
                packageState is PredefinedPackagesState.Data && packageState.carrierPackages.isEmpty() -> EmptyPackages(
                    modifier = modifier,
                    R.drawable.ic_delivery,
                    R.string.woo_shipping_labels_package_creation_empty_carrier_message
                ) { onTabChange(PageType.CUSTOM) }

                packageState is PredefinedPackagesState.Data -> WooShippingCarrierPackageContent(
                    modifier = modifier,
                    carrierPackages = packageState.carrierPackages,
                    onPackageSelected = onPackageSelected,
                )

                packageState is PredefinedPackagesState.Error -> ErrorMessageWithButton(
                    modifier = modifier,
                    message = R.string.woo_shipping_labels_package_creation_carrier_loading_error,
                    onRetryClick = onRetryClick
                )

                packageState is PredefinedPackagesState.Waiting -> Column(
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
fun EmptyPackages(
    modifier: Modifier = Modifier,
    @DrawableRes image: Int,
    @StringRes message: Int,
    onButtonClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(id = image), contentDescription = null)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            stringResource(message),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.height(37.dp))
        WCColoredButton(onClick = onButtonClick) {
            Text(stringResource(id = R.string.woo_shipping_labels_package_creation_empty_button))
        }
    }
}

@Composable
fun WooShippingCarrierPackageContent(
    modifier: Modifier = Modifier,
    carrierPackages: Map<Carrier, List<CarrierPackageGroup>>,
    onPackageSelected: (PackageData, Boolean) -> Unit,
) {
    val pagerState = rememberPagerState { carrierPackages.keys.size }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        CarrierTabRow(
            modifier = modifier,
            pagerState = pagerState,
            carriers = carrierPackages.keys.toList()
        )
        Divider(modifier = Modifier.fillMaxWidth())
        PackageListPager(
            modifier = modifier
                .weight(1f),
            pagerState = pagerState,
            carrierPackages = carrierPackages,
            onPackageSelected = onPackageSelected
        )
    }
}

@Composable
private fun CarrierTabRow(
    modifier: Modifier,
    pagerState: PagerState,
    carriers: List<Carrier>
) {
    val scope = rememberCoroutineScope()
    ScrollableTabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = pagerState.currentPage,
        edgePadding = dimensionResource(R.dimen.major_100),
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.primary,
        divider = {}
    ) {
        carriers.forEachIndexed { index, carrier ->
            val textColor = if (index == pagerState.currentPage) {
                MaterialTheme.colors.primary
            } else {
                colorResource(id = R.color.color_on_surface_medium)
            }
            LeadingIconTab(
                text = {
                    Text(
                        text = carrier.name,
                        color = textColor,
                        style = MaterialTheme.typography.subtitle2
                    )
                },
                icon = {
                    carrier.logoRes?.let { CarrierLogo(it) }
                },
                selected = pagerState.currentPage == index,
                onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        }
    }
}

@Composable
private fun PackageListPager(
    modifier: Modifier,
    pagerState: PagerState,
    carrierPackages: Map<Carrier, List<CarrierPackageGroup>>,
    onPackageSelected: (PackageData, Boolean) -> Unit
) {
    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) { page ->
        val carrierForPageIndex = carrierPackages.keys.toList()[page]
        val carrierPackageGroups = carrierPackages[carrierForPageIndex] ?: emptyList()
        PackageList(
            packageGroups = carrierPackageGroups,
            onPackageSelected = onPackageSelected
        )
    }
}

@Composable
private fun PackageList(
    packageGroups: List<CarrierPackageGroup>,
    onPackageSelected: (PackageData, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()
    ) {
        packageGroups.forEach { group ->
            Spacer(modifier = Modifier.height(8.dp))
            PackageListSection(
                sectionHeader = group.groupName,
                packages = group.packages,
                onPackageSelected = onPackageSelected
            )
        }
    }
}

@Composable
private fun PackageListSection(
    sectionHeader: String,
    packages: List<PackageData>,
    onPackageSelected: (PackageData, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(start = 8.dp)
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = sectionHeader,
            style = MaterialTheme.typography.body1,
            color = colorResource(id = R.color.color_on_surface_disabled)
        )
        Divider()
        packages.forEach { packageData ->
            WooShippingPackageListItem(
                modifier = Modifier.padding(start = 16.dp),
                packageData = packageData,
                onPackageSelected = onPackageSelected
            )
        }
    }
}

@Composable
private fun CarrierLogo(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = resId),
        contentDescription = null,
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(5.dp)),
        tint = Color.Unspecified
    )
}

@Preview
@Composable
fun WooShippingCarrierPackageScreenPreview() {
    WooThemeWithBackground {
        WooShippingCarrierPackageContent(
            carrierPackages = mapOf(
                Carrier.DHL to listOf(
                    CarrierPackageGroup(
                        groupName = "Group 1",
                        packages = listOf(
                            PackageData(
                                name = "Package 1 - Carrier 1",
                                dimensions = "10 x 10 x 10",
                                weight = "10",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            ),
                            PackageData(
                                name = "Package 2 - Carrier 1",
                                dimensions = "20 x 20 x 20",
                                weight = "20",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            )
                        )
                    ),
                    CarrierPackageGroup(
                        groupName = "Group 2",
                        packages = listOf(
                            PackageData(
                                name = "Package 3 - Carrier 1",
                                dimensions = "30 x 30 x 30",
                                weight = "30",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            ),
                            PackageData(
                                name = "Package 4 - Carrier 1",
                                dimensions = "40 x 40 x 40",
                                weight = "40",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            )
                        )
                    )
                ),
                Carrier.USPS to listOf(
                    CarrierPackageGroup(
                        groupName = "Group 2",
                        packages = listOf(
                            PackageData(
                                name = "Package 1 - Carrier 2",
                                dimensions = "10 x 10 x 10",
                                weight = "10",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            ),
                            PackageData(
                                name = "Package 2 Carrier - 2",
                                dimensions = "20 x 20 x 20",
                                weight = "20",
                                isSelected = false,
                                isLetter = false,
                                id = "1",
                            )
                        )
                    )
                )
            ),
            onPackageSelected = { _, _ -> }
        )
    }
}

@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun WooShippingCarrierPackageEmptyScreenPreview() {
    WooThemeWithBackground {
        WooShippingCarrierPackageScreen(
            packageState = PredefinedPackagesState.Data(
                storeOptions = StoreOptionsForPackages.DEFAULT,
                savedPackages = emptyList(),
                carrierPackages = emptyMap()
            ),
            onPackageSelected = { _, _ -> },
            onRetryClick = {},
            onTabChange = {}
        )
    }
}
