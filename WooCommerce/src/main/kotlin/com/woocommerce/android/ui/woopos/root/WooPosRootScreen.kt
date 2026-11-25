package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.roles.approval.LocalWooPosRequestApproval
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalAction
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalDialog
import com.woocommerce.android.ui.woopos.roles.approval.WooPosApprovalViewModel
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
    approvalViewModel: WooPosApprovalViewModel = hiltViewModel(),
) {
    val rootController = rememberNavController()
    val activity = LocalActivity.current as ComponentActivity
    val homeViewModel: WooPosHomeViewModel = hiltViewModel()
    val lockState by lockViewModel.state.collectAsState()
    val approvalState by approvalViewModel.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { approvalViewModel.dismiss() }
    DisposableEffect(rootController, approvalViewModel) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ -> approvalViewModel.dismiss() }
        rootController.addOnDestinationChangedListener(listener)
        onDispose {
            rootController.removeOnDestinationChangedListener(listener)
            approvalViewModel.dismiss()
        }
    }

    WooPosRootScreen(state = lockState, onUIEvent = lockViewModel::onUIEvent, modifier = modifier) {
        // Gate the default Activity finish when no destination handles Back.
        BackHandler { approvalViewModel.requestApproval(WooPosApprovalAction.Exit, activity::finish) }
        CompositionLocalProvider(
            LocalWooPosRequestApproval provides { action, perform ->
                approvalViewModel.requestApproval(action, perform)
            },
        ) {
            WooPosRootHost(
                modifier = Modifier.fillMaxSize(),
                rootController = rootController,
                homeViewModel = homeViewModel,
                onNavigationEvent = { event ->
                    rootController.handleNavigationEvent(event) { exit ->
                        approvalViewModel.requestExit(
                            event = exit,
                            onShowConfirmation = homeViewModel::showExitConfirmation,
                            onExitFromHome = {
                                if (!activity.isFinishing) {
                                    homeViewModel.onExitConfirmed()
                                    activity.finish()
                                }
                            },
                            onExit = activity::finish,
                        )
                    }
                }
            )
        }
        val request = approvalState
        if (!lockState.isLocked && request != null) {
            WooPosApprovalDialog(request, approvalViewModel::onUIEvent)
        }
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
