package com.woocommerce.android.ui.woopos.common.composeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Remembers the last non-null value of [current].
 * Useful for keeping dialog data available during exit animations.
 */
@Composable
fun <T : Any> rememberRetained(current: T?): T? {
    var retained by remember { mutableStateOf(current) }
    if (current != null) retained = current
    return retained
}
