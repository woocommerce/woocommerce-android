package com.woocommerce.android.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import java.util.concurrent.atomic.AtomicBoolean

fun <T> Fragment.handleResult(key: String, handler: (T) -> Unit) {
    findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Pair<T, AtomicBoolean>>(key)?.observe(
        this.viewLifecycleOwner,
        Observer {
            val isFresh = it.second.getAndSet(false)
            if (isFresh) {
                handler(it.first)
            }
        }
    )
}

fun <T> Fragment.navigateBackWithResult(key: String, result: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, Pair(result, AtomicBoolean(true)))
    findNavController().navigateUp()
}
