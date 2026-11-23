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
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalDialog
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalViewModel
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenDialog
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenUIEvent
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen(
    modifier: Modifier = Modifier,
    lockViewModel: WooPosLockScreenViewModel = hiltViewModel(),
    approval: WooPosManagerApprovalViewModel = hiltViewModel(),
) {
    val rootController = rememberNavController()
    val activity = LocalActivity.current as ComponentActivity
    val homeViewModel: WooPosHomeViewModel = hiltViewModel()
    val lockState by lockViewModel.state.collectAsState()
    val approvalState by approval.state.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { approval.cancel() }
    DisposableEffect(rootController, approval) {
        val listener = NavController.OnDestinationChangedListener { _, _, _ -> approval.cancel() }
        rootController.addOnDestinationChangedListener(listener)
        onDispose {
            rootController.removeOnDestinationChangedListener(listener)
            approval.cancel()
        }
    }

    WooPosRootScreen(state = lockState, onUIEvent = lockViewModel::onUIEvent, modifier = modifier) {
        BackHandler { approval.run(WooPosApprovalAction.Exit, activity::finish) }
        CompositionLocalProvider(
            LocalWooPosRequestApproval provides { action, perform -> approval.run(action, perform) },
        ) {
            WooPosRootHost(
                modifier = Modifier.fillMaxSize(),
                rootController = rootController,
                homeViewModel = homeViewModel,
                onNavigationEvent = { event ->
                    rootController.handleNavigationEvent(event) { exit ->
                        val finishHome: () -> Unit = {
                            if (!activity.isFinishing) {
                                homeViewModel.onExitConfirmed()
                                activity.finish()
                            }
                        }
                        when (exit) {
                            WooPosNavigationEvent.RequestExitFromHome -> approval.run(
                                WooPosApprovalAction.Exit,
                                onAllowed = homeViewModel::showExitConfirmation,
                                onApproved = finishHome,
                            )
                            WooPosNavigationEvent.ConfirmExitFromHome ->
                                approval.run(WooPosApprovalAction.Exit, finishHome)
                            WooPosNavigationEvent.ExitPosClicked, WooPosNavigationEvent.BackFromSplashClicked ->
                                approval.run(WooPosApprovalAction.Exit, activity::finish)
                        }
                    }
                }
            )
        }
        val request = approvalState
        if (!lockState.isLocked && request != null) {
            WooPosManagerApprovalDialog(request, approval::onDigit, approval::onDelete, approval::cancel)
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
