package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageTab
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.CARRIER
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.CUSTOM
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.SAVED
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.forms.WooShippingCarrierPackageScreen
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.forms.WooShippingCustomPackageCreationScreen
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.forms.WooShippingSavedPackageScreen

@Composable
fun WooShippingLabelsPackageCreationScreen(
    viewModel: WooShippingLabelsPackageCreationViewModel
) {
    val viewState = viewModel.viewState.observeAsState()
    WooShippingLabelsPackageCreationScreen(
        tabs = viewState.value?.pageTabs.orEmpty()
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WooShippingLabelsPackageCreationScreen(
    modifier: Modifier = Modifier,
    tabs: List<PageTab>
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
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, pageTab ->
                    Tab(
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
                    .verticalScroll(rememberScrollState())
            ) { currentPageIndex ->
                when (tabs[currentPageIndex].type) {
                    CUSTOM -> WooShippingCustomPackageCreationScreen()
                    CARRIER -> WooShippingCarrierPackageScreen()
                    SAVED -> WooShippingSavedPackageScreen()
                }
            }
        }
    )
}

@Preview
@Composable
fun WooShippingLabelsPackageCreationScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelsPackageCreationScreen(
            tabs = listOf(
                PageTab("Custom", CUSTOM),
                PageTab("Carrier", CARRIER),
                PageTab("Saved", SAVED)
            )
        )
    }
}
