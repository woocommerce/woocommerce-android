package com.woocommerce.android.ui.woopos.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
@Suppress("UnusedParameter")
fun WooPosSettingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    viewModel: WooPosSettingsViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = "Settings Screen",
            style = WooPosTypography.Heading,
        )
    }
}
