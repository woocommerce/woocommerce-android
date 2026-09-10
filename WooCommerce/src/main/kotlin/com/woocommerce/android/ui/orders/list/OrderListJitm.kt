package com.woocommerce.android.ui.orders.list

import androidx.compose.runtime.Composable
import com.woocommerce.android.ui.jitm.JitmBanner
import com.woocommerce.android.ui.jitm.JitmModal
import com.woocommerce.android.ui.jitm.JitmState

@Composable
internal fun OrderListJitm(state: JitmState) {
    when (state) {
        is JitmState.Banner -> JitmBanner(state)
        is JitmState.Modal -> JitmModal(state)
        JitmState.Hidden -> Unit
    }
}
