package com.woocommerce.android.ui.woopos.home.items.customamount

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
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

    if (state.screen == WooPosCustomAmountDialogState.Screen.AMOUNT) {
        WooPosCustomAmountAmountScreen(
            state = state,
            onAmountChanged = viewModel::onAmountChanged,
            onContinueClicked = viewModel::onAmountConfirmed,
            onBackClicked = goBack,
            modifier = modifier.background(MaterialTheme.colorScheme.surface),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        WooPosToolbar(
            titleText = stringResource(R.string.woopos_custom_amount_form_title),
            onBackClicked = viewModel::onEditAmount,
        )
        WooPosCustomAmountFormScreen(
            state = state,
            onAmountChanged = viewModel::onAmountChanged,
            onNameChanged = viewModel::onNameChanged,
            onTaxableToggled = viewModel::onTaxableToggled,
            onSubmit = { viewModel.onSubmit(goBack) },
            onBackClick = viewModel::onEditAmount,
            modifier = Modifier.weight(1f),
        )
    }
}
