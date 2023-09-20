package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProductPreviewSubScreen(viewModel: ProductPreviewSubViewModel, modifier: Modifier) {
    viewModel.state.observeAsState().value?.let { state ->
        ProductPreviewSubScreen(state, modifier)
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
private fun ProductPreviewSubScreen(state: ProductPreviewSubViewModel.State, modifier: Modifier) {

}

@Composable
@Preview
private fun ProductPreviewLoadingPreview() {
    WooThemeWithBackground {
        ProductPreviewSubScreen(ProductPreviewSubViewModel.State.Loading, Modifier.fillMaxSize())
    }
}
