package com.woocommerce.android.ui.woopos.root

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost

@Composable
fun WooPosRootScreen() {
    val viewModel: WooPosRootViewModel = hiltViewModel()
    WooPosRootScreen(viewModel::onUiEvent)
}

@Composable
private fun WooPosRootScreen(onUIEvent: (WooPosRootUIEvents) -> Unit) {
    WooPosTheme {
        Column(modifier = Modifier.background(Color.Black)) {
            WooPosRootHost(modifier = Modifier.weight(1f))
            WooPosBottomToolbar(onUIEvent)
        }
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme {
        WooPosRootScreen({})
    }
}
