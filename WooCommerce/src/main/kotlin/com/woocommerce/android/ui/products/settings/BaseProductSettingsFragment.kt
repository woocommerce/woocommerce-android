package com.woocommerce.android.ui.products.settings

import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.wordpress.android.util.ActivityUtils

/**
 * All fragments shown from the main product settings fragment should extend this class.
 * The main settings fragment extends from BaseProductFragment and handles all the
 * communication with the shared product ViewModel. Fragments which extend this are
 * expected to be lightweight.
 */
abstract class BaseProductSettingsFragment : BaseFragment, BackPressListener {
    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }

    override fun onRequestAllowBackPress(): Boolean {
        if (hasChanges()) {
            if (validateChanges()) {
                navigateBackWithResult()
            }
        } else {
            findNavController().navigateUp()
        }
        return false
    }

    /**
     * Navigates back to the main product settings fragment and passes it a bundle containing the changes
     */
    private fun navigateBackWithResult() {
        val (key, result) = getChangesResult()
        navigateBackWithResult(key, result)
    }

    /**
     * Descendants should override this to return a Pair with a key and the result to pass
     */
    abstract fun getChangesResult(): Pair<String, Any>

    /**
     * Descendants should override this to return true if changes have been made
     */
    abstract fun hasChanges(): Boolean

    /**
     * Descendants should override this to validate any changes and return true if validation passes
     */
    abstract fun validateChanges(): Boolean
}
