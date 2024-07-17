package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConfirmationDialog
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosRootViewModel = hiltViewModel()
    WooPosRootScreen(
        modifier = modifier,
        state = viewModel.rootScreenState.collectAsState(),
        onUIEvent = viewModel::onUiEvent,
    )
}

@Composable
private fun WooPosRootScreen(
    modifier: Modifier = Modifier,
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit
) {
    val rootController = rememberNavController()
    val activity = LocalContext.current as ComponentActivity

    state.value.exitConfirmationDialog?.let {
        WooPosConfirmationDialog(
            title = stringResource(id = it.title),
            message = stringResource(id = it.message),
            confirmButtonText = stringResource(id = it.confirmButton),
            dismissButtonText = stringResource(id = it.dismissButton),
            onDismiss = { onUIEvent(WooPosRootUIEvent.ExitConfirmationDialogDismissed) },
            onConfirm = {
                rootController.handleNavigationEvent(
                    WooPosNavigationEvent.ExitPosClicked,
                    activity,
                    onUIEvent
                )
            }
        )
    }

    Box(modifier = modifier.background(MaterialTheme.colors.background)) {
        WooPosRootHost(
            modifier = Modifier.fillMaxSize(),
            rootController = rootController,
            onNavigationEvent = { event ->
                rootController.handleNavigationEvent(event, activity, onUIEvent)
            }
        )
        WooPosBottomToolbar(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .align(Alignment.BottomStart),
            state,
            onUIEvent,
        )
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosRootScreenState.WooPosCardReaderStatus.NotConnected,
                null,
            )
        )
    }
    WooPosTheme {
        WooPosRootScreen(
            state = state,
            onUIEvent = {}
        )
    }
}
