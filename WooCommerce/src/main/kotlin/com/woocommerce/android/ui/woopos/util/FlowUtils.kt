package com.woocommerce.android.ui.woopos.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

/**
 * Convert a [Flow] to [StateFlow].
 *
 * This uses a policy of keeping the upstream active for 5 seconds after disappearance of last collector
 * to avoid restarting the Flow during configuration changes.
 */
@Suppress("MagicNumber")
fun <T> Flow<T>.toStateFlow(scope: CoroutineScope, initialValue: T) = stateIn(
    scope = scope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = initialValue
)