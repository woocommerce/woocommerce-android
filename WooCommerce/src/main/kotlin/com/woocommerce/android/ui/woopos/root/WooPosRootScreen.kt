package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenDialog
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenUIEvent
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

    WooPosRootScreen(state = lockState, onUIEvent = lockViewModel::onUIEvent, modifier = modifier) {
        WooPosRootHost(
            modifier = Modifier.fillMaxSize(),
            rootController = rootController,
            onNavigationEvent = { event ->
                rootController.handleNavigationEvent(event, activity)
            }
        )
    }
}

@Composable
internal fun WooPosRootScreen(
    state: WooPosLockScreenState,
    onUIEvent: (WooPosLockScreenUIEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val background = MaterialTheme.colorScheme.surface
    Box(
        modifier.fillMaxSize().drawWithContent {
            // The Activity must stay opaque while Android recreates the separate lock dialog on rotation.
            if (state.isLocked) drawRect(background) else drawContent()
        }
    ) {
        content()
    }
    WooPosLockScreenDialog(state = state, onUIEvent = onUIEvent)
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme { WooPosRootScreen() }
}
