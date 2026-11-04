package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

@Composable
fun WooPosCustomAmountScreen(
    editing: WooPosCartItemViewState.CustomAmount?,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WooPosCustomAmountDialogViewModel = hiltViewModel(),
) {
    val goBack = { onNavigationEvent(WooPosNavigationEvent.GoBack) }

    LaunchedEffect(editing?.itemNumber) {
        viewModel.initializeFor(editing)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.onDismissed() }
    }
    val state by viewModel.state.collectAsState()

    when (state.screen) {
        WooPosCustomAmountDialogState.Screen.AMOUNT -> {
            WooPosCustomAmountValueScreen(
                state = state,
                onAmountChanged = viewModel::onAmountChanged,
                onContinueClicked = viewModel::onAmountConfirmed,
                onBackClicked = { viewModel.onAmountBack(goBack) },
                modifier = modifier.background(MaterialTheme.colorScheme.surface),
            )
        }

        WooPosCustomAmountDialogState.Screen.DETAILS -> {
            WooPosCustomAmountDetailsScreen(
                state = state,
                onEditAmount = viewModel::onEditAmount,
                onNameChanged = viewModel::onNameChanged,
                onTaxableToggled = viewModel::onTaxableToggled,
                onSubmit = { viewModel.onSubmit(goBack) },
                onBackClicked = goBack,
                modifier = modifier.background(MaterialTheme.colorScheme.surface),
            )
        }
    }
}
