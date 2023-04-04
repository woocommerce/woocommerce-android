package com.woocommerce.android.ui.login.storecreation.summary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton

@Composable
fun StoreCreationSummaryScreen(viewModel: StoreCreationSummaryViewModel) {
    Scaffold(topBar = {
        ToolbarWithHelpButton(
            onNavigationButtonClick = viewModel::onCancelPressed,
            onHelpButtonClick = viewModel::onHelpPressed,
        )
    }) {
        Column(modifier = Modifier.padding(it)) {
            Text("Hello World!")
        }
    }
}
