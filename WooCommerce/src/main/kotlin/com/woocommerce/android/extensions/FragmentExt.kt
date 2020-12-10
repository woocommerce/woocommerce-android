package com.woocommerce.android.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController

/**
 * A helper function that sets the submitted key-value pair in the Fragment's SavedStateHandle. The value can be
 * observed as a LiveData using the same key - see the Fragment.handleResult() extension function. This mechanism is
 * used to facilitate the request-result communication between 2 separate fragments.
 *
 * @param [key] A unique string that is the same as the one used in [handleResult]
 * @param [result] A result value to be returned
 *
 */
fun <T> Fragment.navigateBackWithResult(key: String, result: T) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, result)
    findNavController().navigateUp()
}

fun Fragment.navigateBackWithNotice(key: String) {
    findNavController().previousBackStackEntry?.savedStateHandle?.set(key, key)
    findNavController().navigateUp()
}


/**
 * A helper function that subscribes a supplied handler function to the Fragment's SavedStateHandle LiveData associated
 * with the supplied key.
 *
 * @param [key] A unique string that is the same as the one used in [navigateBackWithResult]
 * @param [entryId] An optional ID to identify the correct back stack entry. It's required when calling [handleResult]
 *  from TopLevelFragment or Dialog (otherwise the result will get lost upon configuration change)
 * @param [handler] A result handler
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

    entry?.savedStateHandle?.let { saveState ->
        saveState.getLiveData<T>(key).observe(
            this.viewLifecycleOwner,
            Observer {
                saveState.remove<T>(key)
                handler(it)
            }
        )
    }
}

fun Fragment.handleNotice(key: String, entryId: Int? = null, handler: () -> Unit) {
    val entry = if (entryId != null) {
        findNavController().getBackStackEntry(entryId)
    } else {
        findNavController().currentBackStackEntry
    }

    entry?.savedStateHandle?.let { saveState ->
        saveState.getLiveData<String>(key).observe(
            this.viewLifecycleOwner,
            Observer {
                saveState.remove<String>(key)
                handler()
            }
        )
    }
}
