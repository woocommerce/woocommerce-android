package com.woocommerce.android.ui.products.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.LayoutRes
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
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
    companion object {
        private const val KEY_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    constructor() : super()
    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    private var isConfirmingDiscard = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        if (savedInstanceState?.getBoolean(KEY_IS_CONFIRMING_DISCARD) == true) {
            confirmDiscard()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_done)?.isVisible = hasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                if (hasChanges()) {
                    if (validateChanges()) {
                        navigateBackWithResult()
                    }
                } else {
                    findNavController().navigateUp()
                }
                true
            }
        else ->
            super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }

    override fun onRequestAllowBackPress(): Boolean {
        if (hasChanges()) {
            confirmDiscard()
            return false
        }
        return true
    }

    private fun confirmDiscard() {
        isConfirmingDiscard = true
        WooDialog.showDialog(
                requireActivity(),
                messageId = R.string.discard_message,
                positiveButtonId = R.string.discard,
                posBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                    findNavController().navigateUp()
                },
                negativeButtonId = R.string.keep_editing,
                negBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                })
    }

    /**
     * Called when the Done button is tapped and changes have been made. Navigates back to the main product
     * settings fragment and passes it a bundle containing the changes.
     */
    private fun navigateBackWithResult() {
        val (key, result) = getChangesResult()
        navigateBackWithResult(key, result)
    }

    /**
     * Descendants should call this when edits are made so we can show/hide the done button
     */
    fun changesMade() {
        activity?.invalidateOptionsMenu()
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
