package com.woocommerce.android.ui.products

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun ProductSharingBottomSheet(viewModel: ProductSharingViewModel) {
    Text(text = viewModel.toString())
}
