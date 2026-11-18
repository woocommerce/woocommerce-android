package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import kotlinx.coroutines.delay

@Composable
fun WooPosCustomAmountScreen(
    mode: WooPosCustomAmountFormState.Mode,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosCustomAmountFormViewModel = hiltViewModel(),
) {
    val close = { onNavigationEvent(WooPosNavigationEvent.CloseCustomAmount) }

    // The view model is scoped to this route's back stack entry, so each opening gets a fresh one. It must not be
    // reset on dispose: rotation recreates the activity and would otherwise wipe the merchant's input and step.
    LaunchedEffect(mode) {
        viewModel.initializeFor(mode)
    }
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSubmitted, state.isClosed) {
        if (state.isSubmitted || state.isClosed) close()
    }

    when (state.screen) {
        WooPosCustomAmountFormState.Screen.AMOUNT -> {
            WooPosCustomAmountValueScreen(
                state = state,
                onAmountChanged = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.AmountChanged(it)) },
                onSaveClicked = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.AmountConfirmed) },
                onBackClicked = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.AmountBackClicked) },
                modifier = modifier,
            )
        }

        WooPosCustomAmountFormState.Screen.NAME -> {
            WooPosCustomAmountNameScreen(
                value = state.nameDraft,
                onNameChanged = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.NameDraftChanged(it)) },
                onSaveClicked = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.NameConfirmed) },
                onBackClicked = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.NameBackClicked) },
                modifier = modifier,
            )
        }

        WooPosCustomAmountFormState.Screen.DETAILS -> {
            // The amount and name Save buttons share a position with this Save button, so the second tap of a fast
            // double tap would land here and add the item before the merchant sees the details.
            val doubleTapTimeoutMillis = LocalViewConfiguration.current.doubleTapTimeoutMillis
            var acceptsSubmit by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(doubleTapTimeoutMillis)
                acceptsSubmit = true
            }
            WooPosCustomAmountDetailsScreen(
                state = state,
                onEditAmount = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.EditAmountClicked) },
                onEditName = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.EditNameClicked) },
                onTaxableToggled = { viewModel.onUIEvent(WooPosCustomAmountUIEvent.TaxableToggled(it)) },
                onSubmit = {
                    if (acceptsSubmit) viewModel.onUIEvent(WooPosCustomAmountUIEvent.SubmitClicked)
                },
                onBackClicked = close,
                modifier = modifier,
            )
        }
    }
}
