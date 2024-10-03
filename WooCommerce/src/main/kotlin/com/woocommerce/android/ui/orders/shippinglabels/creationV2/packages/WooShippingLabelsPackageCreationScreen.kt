package com.woocommerce.android.ui.orders.shippinglabels.creationV2.packages

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WooShippingLabelsPackageCreationScreen(
    viewModel: WooShippingLabelsPackageCreationViewModel
) {
    viewModel.apply {  }
    WooShippingLabelsPackageCreationScreen()
}

@Composable
fun WooShippingLabelsPackageCreationScreen(
    modifier: Modifier = Modifier
) {
    Column {
        Text(text = "WooShippingLabelsPackageCreationScreen")
    }
}
