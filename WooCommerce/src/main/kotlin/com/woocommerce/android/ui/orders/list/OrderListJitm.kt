package com.woocommerce.android.ui.orders.list

import androidx.compose.runtime.Composable
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.jitm.JitmModal
import com.woocommerce.android.ui.jitm.JitmState
import com.woocommerce.android.ui.payments.banner.Banner

@Composable
internal fun OrderListJitm(state: JitmState) {
    WooThemeWithBackground {
        when (state) {
            is JitmState.Banner -> Banner(state)
            is JitmState.Modal -> JitmModal(state)
            JitmState.Hidden -> Unit
        }
    }
}
