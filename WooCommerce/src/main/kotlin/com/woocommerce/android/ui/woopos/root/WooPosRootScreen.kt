package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen(modifier: Modifier = Modifier) {
    val rootController = rememberNavController()
    val activity = LocalActivity.current as ComponentActivity

    WooPosRootHost(
        modifier = modifier.fillMaxSize(),
        rootController = rootController,
        onNavigationEvent = { event ->
            rootController.handleNavigationEvent(event, activity)
        }
    )
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme { WooPosRootScreen() }
}
