package com.woocommerce.android.ui.login.storecreation.themes

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier

@Composable
fun ThemePickerScreen(viewModel: ThemePickerViewModel) {
    viewModel.viewState.observeAsState().value?.let { _ ->
        ThemePickerScreenCarousel(
            modifier = Modifier,
        )
    }
}

@Composable
private fun ThemePickerScreenCarousel(
    modifier: Modifier,
) {
    Text(modifier = modifier, text = "Choose a theme")
}
