package com.woocommerce.android

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs

/**
 * Creates a SavedStateHandel with the intial state matching the passed arguments
 */
fun NavArgs.initSavedStateHandle(): SavedStateHandle {
    val toBundleMethod = javaClass.getDeclaredMethod("toBundle")
    val bundle = toBundleMethod.invoke(this) as Bundle
    val argumentsMap = bundle.keySet().map { it to bundle[it] }.toMap()
    return SavedStateHandle(argumentsMap)
}
