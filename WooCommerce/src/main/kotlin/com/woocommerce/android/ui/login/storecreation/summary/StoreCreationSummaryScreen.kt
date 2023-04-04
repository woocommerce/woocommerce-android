package com.woocommerce.android.ui.login.storecreation.summary

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.ui.compose.component.ToolbarWithHelpButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreCreationSummaryScreen(viewModel: StoreCreationSummaryViewModel) {
    StoreCreationSummaryScreen(
        onCancelPressed = viewModel::onCancelPressed,
        onHelpPressed = viewModel::onHelpPressed
    )
}


@Composable
private fun StoreCreationSummaryScreen(
    onCancelPressed: () -> Unit,
    onHelpPressed: () -> Unit
) {
    Scaffold(topBar = {
        ToolbarWithHelpButton(
            onNavigationButtonClick = onCancelPressed,
            onHelpButtonClick = onHelpPressed,
        )
    }) {
        Column(modifier = Modifier.padding(it)) {
            Text("Hello World!")
        }
    }
}

@Preview(name = "dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun StoreCreationSummary() {
    WooThemeWithBackground {
        StoreCreationSummaryScreen(
            onCancelPressed = {},
            onHelpPressed = {}
        )
    }
}
