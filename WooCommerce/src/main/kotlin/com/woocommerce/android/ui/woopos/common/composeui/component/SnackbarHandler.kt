package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.home.totals.SnackbarMessage
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsState
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent

@Composable
fun SnackbarHandler(
    state: WooPosTotalsState,
    snackbarHostState: SnackbarHostState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    val snackbarState = state.snackbarMessage
    if (snackbarState is SnackbarMessage.Triggered) {
        val message = stringResource(snackbarState.message)
        LaunchedEffect(state.snackbarMessage) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.Dismissed -> {
                    onUIEvent(WooPosTotalsUIEvent.SnackbarDismissed)
                }

                SnackbarResult.ActionPerformed -> Unit
            }
        }
    }
}