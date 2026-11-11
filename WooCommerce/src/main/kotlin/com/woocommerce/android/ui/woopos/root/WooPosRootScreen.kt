package com.woocommerce.android.ui.woopos.root

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenDialog
import com.woocommerce.android.ui.woopos.lock.WooPosLockScreenViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
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
    LaunchedEffect(lockViewModel) {
        lockViewModel.exitPOS.collect {
            if (activity.isTaskRoot) activity.startActivity(Intent(activity, MainActivity::class.java))
            activity.finish()
        }
    }

    WooPosRootHost(
        modifier = modifier.fillMaxSize().then(
            if (lockState.isLocked) Modifier.clearAndSetSemantics {} else Modifier
        ),
        rootController = rootController,
        onNavigationEvent = { event ->
            when (event) {
                WooPosNavigationEvent.ExitPosClicked,
                WooPosNavigationEvent.BackFromSplashClicked -> lockViewModel.requestExitPOS()
                else -> rootController.handleNavigationEvent(event, activity)
            }
        }
    )
    if (lockState.isLocked) {
        BackHandler {}
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
        )
    }
    WooPosLockScreenDialog(state = lockState, onUIEvent = lockViewModel::onUIEvent)
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    WooPosTheme { WooPosRootScreen() }
}
