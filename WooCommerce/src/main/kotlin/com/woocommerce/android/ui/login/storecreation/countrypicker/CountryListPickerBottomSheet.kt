package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun CountryPickerSelectorBottomSheet(viewModel: CountryListPickerViewModel) {
    viewModel.countryListPickerState.observeAsState().value?.let { viewState ->
        Text(text = viewState.toString())
    }
}
