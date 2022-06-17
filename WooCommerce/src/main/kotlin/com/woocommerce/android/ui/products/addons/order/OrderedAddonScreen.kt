package com.woocommerce.android.ui.products.addons.order

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.products.addons.order.OrderedAddonViewModel.ViewState
import org.wordpress.android.fluxc.domain.Addon

@Composable
fun OrderedAddonScreen(
    viewModel: OrderedAddonViewModel
) {
    val orderedAddonState by viewModel.orderedAddonsData.observeAsState(initial = emptyList())
    val orderedAddonViewState by viewModel.viewStateData.observeAsState(initial = ViewState())

    OrderedAddonScreen(
        orderedAddons = orderedAddonState,
        state = orderedAddonViewState
    )
}

@Composable
fun OrderedAddonScreen(
    orderedAddons: List<Addon>,
    state: ViewState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Ordered Addons should be here")
    }
}
