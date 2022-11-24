package com.woocommerce.android.ui.compose.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.UiHelpers

@Composable
fun UiString.getText(): String {
    return UiHelpers.getTextOfUiString(LocalContext.current, this)
}
