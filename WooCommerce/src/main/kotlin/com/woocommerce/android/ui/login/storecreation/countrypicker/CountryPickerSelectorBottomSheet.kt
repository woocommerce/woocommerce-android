package com.woocommerce.android.ui.login.storecreation.countrypicker

import androidx.compose.material.Text
import androidx.compose.runtime.Composable

@Composable
fun CountryPickerSelectorBottomSheet(viewModel: CountryPickerSelectorViewModel) {
    Text(text = viewModel.toString())
}
