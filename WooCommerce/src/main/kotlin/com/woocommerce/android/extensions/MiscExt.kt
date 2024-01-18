package com.woocommerce.android.extensions

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

inline fun <T> T.takeIfNotEqualTo(other: T?, block: (T) -> Unit) {
    if (this != other) block(this)
}

suspend inline fun <T, R> T.runWithContext(
    context: CoroutineContext,
    crossinline block: (T) -> R
) = withContext(context) {
    block(this@runWithContext)
}
