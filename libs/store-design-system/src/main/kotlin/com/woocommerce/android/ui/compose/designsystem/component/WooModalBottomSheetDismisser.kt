package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Coordinates animated programmatic dismissal while leaving visibility and composition ownership with the caller.
 */
@Stable
class WooModalBottomSheetDismisser internal constructor(
    private val coroutineScope: CoroutineScope,
    private val hide: suspend () -> Unit,
    private val isVisible: () -> Boolean,
    private val onDismissed: () -> Unit,
) {
    var isDismissing by mutableStateOf(false)
        private set

    fun dismiss() {
        if (isDismissing) return

        isDismissing = true
        coroutineScope.launch {
            try {
                hide()
                if (!isVisible()) {
                    onDismissed()
                }
            } finally {
                isDismissing = false
            }
        }
    }
}

/**
 * Remembers a [WooModalBottomSheetDismisser] that invokes [onDismissed] only after [state] is hidden.
 */
@Composable
fun rememberWooModalBottomSheetDismisser(
    state: WooModalBottomSheetState,
    onDismissed: () -> Unit,
): WooModalBottomSheetDismisser {
    val coroutineScope = rememberCoroutineScope()
    val currentOnDismissed = rememberUpdatedState(onDismissed)
    return remember(state, coroutineScope) {
        WooModalBottomSheetDismisser(
            coroutineScope = coroutineScope,
            hide = state::hide,
            isVisible = state::isVisible,
            onDismissed = { currentOnDismissed.value() },
        )
    }
}
