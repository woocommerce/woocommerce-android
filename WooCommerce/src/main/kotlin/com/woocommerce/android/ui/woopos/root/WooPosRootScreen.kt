package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen(modifier: Modifier = Modifier) {
    val rootController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity

    Box(modifier = modifier.background(MaterialTheme.colors.background)) {
        WooPosRootHost(
            modifier = Modifier.fillMaxSize(),
            rootController = rootController,
            onNavigationEvent = { event ->
                rootController.handleNavigationEvent(event, activity)
            }
        )
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme { WooPosRootScreen() }
}
