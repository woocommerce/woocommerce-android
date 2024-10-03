package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageTab
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.CARRIER
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.CUSTOM
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageType.SAVED

@Composable
fun WooShippingLabelsPackageCreationScreen(
    viewModel: WooShippingLabelsPackageCreationViewModel
) {
    val viewState = viewModel.viewState.observeAsState()
    WooShippingLabelsPackageCreationScreen(
        tabs = viewState.value?.pageTabs.orEmpty()
    )
}

@Composable
fun WooShippingLabelsPackageCreationScreen(
    modifier: Modifier = Modifier,
    tabs: List<PageTab>
) {
    var tabIndex by remember { mutableIntStateOf(0) }

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
            Column(
                modifier = modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                val currentTab = tabs[tabIndex]
                when (currentTab.type) {
                    CUSTOM -> {
                        Text(text = "Custom tab content")
                    }
                    CARRIER -> {
                        Text(text = "Carrier tab content")
                    }
                    SAVED -> {
                        Text(text = "Saved tab content")
                    }
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
