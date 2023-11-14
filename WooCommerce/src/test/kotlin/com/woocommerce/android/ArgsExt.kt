package com.woocommerce.android

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs

/**
 * Creates a SavedStateHandel with the initial state matching the passed arguments
 *
 * This function just proxies the call to the generated function `toSavedStateHandel`, we use it
 * just because Android Studio Arctic Fox doesn't support it yet, and highlights it as an error.
 * https://issuetracker.google.com/issues/195753902
 * TODO replace the function with the generated one everywhere on the tests once Android Bumblebee is released
 */
fun NavArgs.initSavedStateHandle(): SavedStateHandle {
    val toSavedStateHandle = javaClass.getDeclaredMethod("toSavedStateHandle")
    return toSavedStateHandle.invoke(this) as SavedStateHandle
}
