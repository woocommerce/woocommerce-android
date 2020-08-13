package com.woocommerce.android.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

fun <T> Fragment.handleResult(key: String, handler: (T) -> Unit) {
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<T>(key)?.observe(
        this.viewLifecycleOwner,
        Observer {
            handler(it)
        }
    )
}

fun <T> Fragment.navigateBackWithResult(key: String, result: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
    findNavController().navigateUp()
}
