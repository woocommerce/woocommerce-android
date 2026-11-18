package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenDialog
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen(
    modifier: Modifier = Modifier,
    lockViewModel: WooPosLockScreenViewModel = hiltViewModel(),
) {
    val rootController = rememberNavController()
    val activity = LocalActivity.current as ComponentActivity
    val lockState by lockViewModel.state.collectAsState()

    WooPosRootHost(
        modifier = modifier.fillMaxSize(),
        rootController = rootController,
        onNavigationEvent = { event ->
            rootController.handleNavigationEvent(event, activity)
        }
    )
    WooPosLockScreenDialog(state = lockState, onUIEvent = lockViewModel::onUIEvent)
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme { WooPosRootScreen() }
}
