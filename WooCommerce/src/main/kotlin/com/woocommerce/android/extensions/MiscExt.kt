package com.woocommerce.android.extensions

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

/**
 * The objective of this function is to make easier the usage of [Pair]
 * as a double valued function return, this way the unwrap delivers inline
 * both arguments inside a declared HOF
 *
 * [F] stands for the first argument of the Pair
 * [S] stands for the second argument of the Pair
 * [R] stands for the response of the action so it can be piped with
 * additional functions if needed
 */
inline fun <F, S, R> Pair<F, S>.unwrap(
    action: (F, S) -> R
) = action(first, second)
