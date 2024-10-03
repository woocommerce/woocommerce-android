package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages.WooShippingLabelsPackageCreationViewModel.PageTab

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
    currentTabIndex: Int = 0,
    tabs: List<PageTab>
) {
    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = currentTabIndex) {
            tabs.forEachIndexed { index, pageTab ->
                Tab(
                    text = { Text(text = pageTab.title) },
                    selected = currentTabIndex == index,
                    onClick = { /*TODO*/ }
                )
            }
        }
    }
}

@Preview
@Composable
fun WooShippingLabelsPackageCreationScreenPreview() {
    WooThemeWithBackground {
        WooShippingLabelsPackageCreationScreen(
            tabs = listOf(
                PageTab("Custom"),
                PageTab("Carrier"),
                PageTab("Saved")
            )
        )
    }
}
