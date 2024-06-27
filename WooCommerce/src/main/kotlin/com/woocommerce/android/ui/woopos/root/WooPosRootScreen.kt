package com.woocommerce.android.ui.woopos.root

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosConfirmationDialog
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.WooPosRootHost
import com.woocommerce.android.ui.woopos.root.navigation.handleNavigationEvent

@Composable
fun WooPosRootScreen() {
    val viewModel: WooPosRootViewModel = hiltViewModel()
    WooPosRootScreen(
        viewModel.rootScreenState.collectAsState(),
        viewModel::onUiEvent
    )
}

@Composable
private fun WooPosRootScreen(
    state: State<WooPosRootScreenState>,
    onUIEvent: (WooPosRootUIEvent) -> Unit
) {
    WooPosTheme {
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

        Column(modifier = Modifier.background(MaterialTheme.colors.background)) {
            WooPosRootHost(
                modifier = Modifier.weight(1f),
                rootController = rootController,
                onNavigationEvent = { event ->
                    rootController.handleNavigationEvent(event, activity, onUIEvent)
                }
            )
            WooPosBottomToolbar(
                state,
                onUIEvent,
            )
        }
    }
}

@WooPosPreview
@Composable
fun PreviewWooPosRootScreen() {
    val state = remember {
        mutableStateOf(
            WooPosRootScreenState(
                WooPosRootScreenState.WooPosCardReaderStatus.Unknown,
                null,
            )
        )
    }
    WooPosTheme {
        WooPosRootScreen(state, {})
    }
}
