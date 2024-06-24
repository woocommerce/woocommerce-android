package com.woocommerce.android.ui.woopos.common.composeui.component.snackbar

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent

@Composable
fun WooPosSnackbar(
    state: WooPosSnackbarState,
    snackbarHostState: SnackbarHostState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    if (state is WooPosSnackbarState.Triggered) {
        val message = stringResource(state.message)
        LaunchedEffect(state) {
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
