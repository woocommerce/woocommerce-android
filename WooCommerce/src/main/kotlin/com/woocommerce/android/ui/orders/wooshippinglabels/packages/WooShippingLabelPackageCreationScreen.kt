package com.woocommerce.android.ui.orders.wooshippinglabels.packages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageTab
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageType.CARRIER
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageType.CUSTOM
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.WooShippingLabelPackageCreationViewModel.PageType.SAVED
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.Carrier
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.CarrierPackageGroup
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.WooShippingCarrierPackageContent
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.WooShippingCarrierPackageScreen
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.WooShippingCustomPackageCreationScreen
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.WooShippingSavedPackageContent
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.WooShippingSavedPackageScreen

@Composable
fun WooShippingLabelPackageCreationScreen(
    viewModel: WooShippingLabelPackageCreationViewModel
) {
    val viewState = viewModel.viewState.observeAsState()
    WooShippingLabelPackageCreationScreen(
        tabs = viewState.value?.pageTabs.orEmpty(),
        createCustomPackageScreen = { WooShippingCustomPackageCreationScreen(viewModel) },
        createCarrierPackageScreen = { WooShippingCarrierPackageScreen(viewModel) },
        createSavedPackageScreen = { WooShippingSavedPackageScreen(viewModel) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WooShippingLabelPackageCreationScreen(
    modifier: Modifier = Modifier,
    tabs: List<PageTab>,
    createCustomPackageScreen: @Composable () -> Unit,
    createCarrierPackageScreen: @Composable () -> Unit,
    createSavedPackageScreen: @Composable () -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState { tabs.size }
    LaunchedEffect(key1 = tabIndex) {
        pagerState.animateScrollToPage(tabIndex)
    }
    LaunchedEffect(key1 = pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            tabIndex = pagerState.currentPage
        }
    }

    Scaffold(
        topBar = {
            TabRow(
                selectedTabIndex = tabIndex,
                backgroundColor = MaterialTheme.colors.surface,
            ) {
                tabs.forEachIndexed { index, pageTab ->
                    Tab(
                        selectedContentColor = MaterialTheme.colors.onSurface,
                        text = { Text(text = pageTab.title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
        },
        content = { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(paddingValues)
                    .fillMaxSize()
            ) { currentPageIndex ->
                when (tabs[currentPageIndex].type) {
                    CUSTOM -> createCustomPackageScreen()
                    CARRIER -> createCarrierPackageScreen()
                    SAVED -> createSavedPackageScreen()
                }
            }
        }
    )
}

@Preview
@Composable
fun WooShippingLabelsPackageCreationScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelPackageCreationScreen(
            tabs = listOf(
                PageTab("Custom", CUSTOM),
                PageTab("Carrier", CARRIER),
                PageTab("Saved", SAVED)
            ),
            createCustomPackageScreen = {
                WooShippingCustomPackageCreationScreen(
                    packageName = "Custom Package Name",
                    packageType = "Envelope",
                    packageLength = "10",
                    packageWidth = "10",
                    packageHeight = "10",
                    packageWeight = "10",
                    isAddPackageEnabled = true,
                    isSaveAsTemplateChecked = true,
                    onAddPackageClick = {},
                    onPackageTypeClick = {},
                    onLengthChange = {},
                    onWidthChange = {},
                    onHeightChange = {},
                    onWeightChange = {},
                    onPackageNameChange = {},
                    onSavePackageChanged = {}
                )
            },
            createSavedPackageScreen = {
                WooShippingSavedPackageContent(
                    savedPackages = listOf(
                        PackageData(
                            name = "Small Flat Rate Box",
                            dimensions = "10 x 10 x 10",
                            weight = "10",
                            isSelected = true,
                            isLetter = true,
                            id = "1",
                        ),
                        PackageData(
                            name = "Small Flat Rate Box",
                            dimensions = "20 x 20 x 20",
                            weight = "10",
                            isSelected = false,
                            isLetter = false,
                            id = "1",
                        ),
                        PackageData(
                            name = "Small Flat Rate Box",
                            dimensions = "30 x 30 x 30",
                            weight = "10",
                            isSelected = false,
                            isLetter = false,
                            id = "1",
                        )
                    ),
                    onSavedPackageSelected = { _, _ -> }
                )
            },
            createCarrierPackageScreen = {
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
        )
    }
}
