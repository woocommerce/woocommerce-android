package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.delay

@Composable
fun WooPosCustomAmountScreen(
    mode: WooPosCustomAmountDialogState.Mode,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosCustomAmountDialogViewModel = hiltViewModel(),
) {
    val close = { onNavigationEvent(WooPosNavigationEvent.CloseCustomAmount) }

    LaunchedEffect(mode) {
        viewModel.initializeFor(mode)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.onUIEvent(WooPosCustomAmountUIEvent.Closed) }
    }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSubmitted) {
        if (state.isSubmitted) close()
    }

    when (state.screen) {
        WooPosCustomAmountDialogState.Screen.AMOUNT -> {
            WooPosCustomAmountValueScreen(
                state = state,
                onAmountChanged = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(it)) },
                onContinueClicked = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed) },
                onBackClicked = close,
                modifier = modifier,
            )
        }

        WooPosCustomAmountDialogState.Screen.FORM -> {
            val editAmount = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked) }
            // Continue and the submit button share a position, so the second tap of a fast double tap
            // on Continue would land on submit and add the item before the merchant sees the form.
            val doubleTapTimeoutMillis = LocalViewConfiguration.current.doubleTapTimeoutMillis
            var acceptsSubmit by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(doubleTapTimeoutMillis)
                acceptsSubmit = true
            }
            Column(
                modifier = modifier.fillMaxSize()
            ) {
                WooPosToolbar(
                    titleText = stringResource(R.string.woopos_custom_amount_form_title),
                    onBackClicked = editAmount,
                )
                WooPosCustomAmountFormScreen(
                    state = state,
                    onUIEvent = { event ->
                        if (event != WooPosCustomAmountUIEvent.SubmitClicked || acceptsSubmit) {
                            viewModel.onUIEvent(event)
                        }
                    },
                    onBackClick = editAmount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
