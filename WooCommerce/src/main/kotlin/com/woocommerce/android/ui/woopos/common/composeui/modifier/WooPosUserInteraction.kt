package com.woocommerce.android.ui.woopos.common.composeui.modifier

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

@Composable
internal fun Modifier.wooPosUserInteraction(): Modifier {
    val activity = LocalActivity.current
    return pointerInput(activity) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.any { it.pressed }) activity?.onUserInteraction()
            }
        }
    }.onPreviewKeyEvent {
        activity?.onUserInteraction()
        false
    }
}
