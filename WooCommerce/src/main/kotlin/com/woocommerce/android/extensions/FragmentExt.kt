package com.woocommerce.android.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import java.util.concurrent.atomic.AtomicBoolean

/*
 * A helper function that sets the submitted key-value pair in the Fragment's SavedStateHandle. The value can be
 * observed as a LiveData using the same key - see the Fragment.handleResult() extension function.
 *
 * This mechanism is used to facilitate the request-result communication between 2 separate fragments.
 *
 * Note: The value is stored along with a boolean value (true), which signifies that the data is fresh and has not been
 * observed yet. AtomicBoolean type is used to store the boolean so that the value can be updated once once the data is
 * handled/observed.
 */
fun <T> Fragment.navigateBackWithResult(key: String, result: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, Pair(result, AtomicBoolean(true)))
    findNavController().navigateUp()
}

/*
 * A helper function that subscribes a supplied handler function to the Fragment's SavedStateHandle LiveData associated
 * with the supplied key.
 *
 * Note: The handler is called only if the value wasn't handled before (i.e. the data is fresh). Once the observer is
 * called, the boolean value is updated. This puts a limit on the number of observers for a particular key-result pair
 * to 1.
 */
fun <T> Fragment.handleResult(key: String, entryId: Int? = null, handler: (T) -> Unit) {
    val entry = if (entryId != null) {
        findNavController().getBackStackEntry(entryId)
    } else {
        findNavController().currentBackStackEntry
    }

    entry?.savedStateHandle?.getLiveData<Pair<T, AtomicBoolean>>(key)?.observe(
        this.viewLifecycleOwner,
        Observer {
            val isFresh = it.second.getAndSet(false)
            if (isFresh) {
                handler(it.first)
            }
        }
    )
}
