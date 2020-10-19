package com.woocommerce.android.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

/*
 * A helper function that sets the submitted key-value pair in the Fragment's SavedStateHandle. The value can be
 * observed as a LiveData using the same key - see the Fragment.handleResult() extension function.
 *
 * This mechanism is used to facilitate the request-result communication between 2 separate fragments.
 *
 */
fun <T> Fragment.navigateBackWithResult(key: String, result: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
    findNavController().navigateUp()
}

/*
 * A helper function that subscribes a supplied handler function to the Fragment's SavedStateHandle LiveData associated
 * with the supplied key.
 *
 * Note: Once the observer is called, the value is removed from the SavedStateHandle so that the handler isn't called
 * repeatedly on device rotation. Another reason is that the value does not need to be serialized.
 * This puts a limit on the number of observers for a particular key-result pair to 1.
 */
fun <T> Fragment.handleResult(key: String, handler: (T) -> Unit) {
    findNavController().currentBackStackEntry?.savedStateHandle?.let { saveState ->
        saveState.getLiveData<T>(key).observe(
            this.viewLifecycleOwner,
            Observer {
                saveState.remove<T>(key)
                handler(it)
            }
        )
    }
}
