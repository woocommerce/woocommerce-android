package com.woocommerce.android.ui.woopos.root

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.roles.approval.LocalWooPosRequestApproval
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalAction
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalDialog
import com.woocommerce.android.ui.woopos.roles.approval.WooPosManagerApprovalViewModel
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenDialog
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenUIEvent
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent
import com.woocommerce.android.ui.woopos.splash.WooPosSplashScreen

@Composable
fun WooPosRootScreen(
    modifier: Modifier = Modifier,
    lockViewModel: WooPosLockScreenViewModel = hiltViewModel(),
    approvalViewModel: WooPosManagerApprovalViewModel = hiltViewModel(),
    prepareSavedSale: Boolean = false,
) {
    val savedController = rememberNavController()
    var preparationPending by remember { mutableStateOf(prepareSavedSale) }
    var restoreNavigation by remember { mutableStateOf(true) }
    val rootController = if (restoreNavigation) savedController else rememberNavController()
    val activity = LocalActivity.current as ComponentActivity
    val homeViewModel: WooPosHomeViewModel = hiltViewModel()
    val lockState by lockViewModel.state.collectAsState()
    val approvalState by approvalViewModel.state.collectAsState()
    val exitRequested by lockViewModel.exitRequested.collectAsState()
    FinishAuthorizedExit(exitRequested)

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
        val exitPOS = lockViewModel::onAuthorizedExit
        BackHandler { approvalViewModel.requestApproval(WooPosManagerApprovalAction.Exit, exitPOS) }
        CompositionLocalProvider(
            LocalWooPosRequestApproval provides { action, perform ->
                approvalViewModel.requestApproval(action, perform)
            },
        ) {
            val navigate: (WooPosNavigationEvent) -> Unit = { event ->
                rootController.handleNavigationEvent(event) { exit ->
                    val finishHome: () -> Unit = {
                        if (!activity.isFinishing) {
                            homeViewModel.onExitConfirmed()
                            exitPOS()
                        }
                    }
                    when (exit) {
                        WooPosNavigationEvent.RequestExitFromHome -> approvalViewModel.requestApproval(
                            WooPosManagerApprovalAction.Exit,
                            onAllowed = homeViewModel::showExitConfirmation,
                            onApproved = finishHome,
                        )
                        WooPosNavigationEvent.ConfirmExitFromHome ->
                            approvalViewModel.requestApproval(WooPosManagerApprovalAction.Exit, finishHome)
                        WooPosNavigationEvent.ExitPosClicked, WooPosNavigationEvent.BackFromSplashClicked ->
                            approvalViewModel.requestApproval(WooPosManagerApprovalAction.Exit, exitPOS)
                    }
                }
            }
            if (preparationPending) {
                WooPosSplashScreen { event ->
                    when (event) {
                        WooPosNavigationEvent.OpenHomeFromSplash -> preparationPending = false
                        is WooPosNavigationEvent.OpenEligibilityScreenFromSplash -> {
                            restoreNavigation = false
                            preparationPending = false
                        }
                        else -> navigate(event)
                    }
                }
            } else {
                WooPosRootHost(
                    modifier = Modifier.fillMaxSize(),
                    rootController = rootController,
                    homeViewModel = homeViewModel,
                    onNavigationEvent = navigate,
                )
            }
        }
        val request = approvalState
        if (!lockState.isLocked && request != null) {
            WooPosManagerApprovalDialog(request, approvalViewModel::onUIEvent)
        }
    }
}

@Composable
private fun FinishAuthorizedExit(exitRequested: Boolean) {
    val activity = LocalActivity.current as ComponentActivity
    LaunchedEffect(exitRequested, activity) {
        if (exitRequested && !activity.isFinishing) {
            if (activity.isTaskRoot) activity.startActivity(Intent(activity, MainActivity::class.java))
            activity.finish()
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
