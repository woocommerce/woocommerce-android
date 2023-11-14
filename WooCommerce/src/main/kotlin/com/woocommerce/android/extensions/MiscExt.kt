package com.woocommerce.android.extensions

import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

inline fun <T> T.takeIfNotEqualTo(other: T?, block: (T) -> Unit) {
    if (this != other) block(this)
}

/**
 * Used to convert when statement into expression. In this case compiler check if all the cases are handled
 *
 * when (sealedClass) {
 *
 *      }.exhaustive
 *
 * https://proandroiddev.com/til-when-is-when-exhaustive-31d69f630a8b
 */
val Any?.exhaustive
    get() = Unit

suspend inline fun <T, R> T.runWithContext(
    context: CoroutineContext,
    crossinline block: (T) -> R
) = withContext(context) {
    block(this@runWithContext)
}
