package com.woocommerce.android.ui.products

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun ProductDescriptionAICelebrationBottomSheet(viewModel: ProductDescriptionAICelebrationViewModel) {
    Text(text = viewModel.toString())
}
