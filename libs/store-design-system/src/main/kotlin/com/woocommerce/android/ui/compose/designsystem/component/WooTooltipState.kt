@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * State for [WooTooltipBox]. Each tooltip host should own a distinct instance.
 *
 * Lifecycle and global one-tooltip coordination follow Material's tooltip state while the experimental Material type
 * remains internal to the design system.
 */
@Stable
class WooTooltipState internal constructor(
    internal val materialState: TooltipState,
) {
    /** Snapshot-backed visibility reported by the wrapped Material tooltip state. */
    val isVisible: Boolean
        get() = materialState.isVisible

    /** Shows this tooltip persistently using Material's global tooltip coordination. */
    suspend fun show() = materialState.show()

    /** Dismisses this tooltip. */
    fun dismiss() = materialState.dismiss()
}

/**
 * Remembers persistent tooltip state. Material coordinates presentation so only one globally coordinated tooltip is
 * shown at a time.
 */
@Composable
fun rememberWooTooltipState(): WooTooltipState {
    val materialState = rememberTooltipState(isPersistent = true)
    return remember(materialState) { WooTooltipState(materialState) }
}
