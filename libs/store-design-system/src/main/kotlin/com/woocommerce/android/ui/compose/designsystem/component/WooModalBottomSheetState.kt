@file:OptIn(ExperimentalMaterial3Api::class)

package com.woocommerce.android.ui.compose.designsystem.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class WooModalBottomSheetState internal constructor(
    internal val materialState: SheetState,
) {
    val isVisible: Boolean
        get() = materialState.isVisible

    suspend fun show() = materialState.show()

    suspend fun hide() = materialState.hide()
}

@Composable
fun rememberWooModalBottomSheetState(
    skipPartiallyExpanded: Boolean = true,
): WooModalBottomSheetState {
    val materialState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    return remember(materialState) { WooModalBottomSheetState(materialState) }
}
