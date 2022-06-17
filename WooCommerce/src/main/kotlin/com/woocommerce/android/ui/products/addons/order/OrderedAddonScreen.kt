package com.woocommerce.android.ui.products.addons.order

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun OrderedAddonScreen(
    viewModel: OrderedAddonViewModel
) {
    val orderedAddonState by viewModel.orderedAddonsData.observeAsState()
}
