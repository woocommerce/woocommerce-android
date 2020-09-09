package com.woocommerce.android.ui.products.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import org.wordpress.android.util.ActivityUtils

/**
 * All fragments shown from the main product settings fragment should extend this class.
 * The main settings fragment extends from BaseProductFragment and handles all the
 * communication with the shared product ViewModel. Fragments which extend this are
 * expected to be lightweight.
 */
abstract class BaseProductSettingsFragment : BaseFragment(), BackPressListener {
    companion object {
        private const val KEY_IS_CONFIRMING_DISCARD = "is_confirming_discard"
    }

    private var isConfirmingDiscard = false

    // descendants should override this with a unique request code
    protected abstract val requestCode: Int

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
        CustomDiscardDialog.onCleared()
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
        CustomDiscardDialog.showDiscardDialog(
                requireActivity(),
                posBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                    findNavController().navigateUp()
                },
                negBtnAction = DialogInterface.OnClickListener { _, _ ->
                    isConfirmingDiscard = false
                })
    }

    /**
     * Called when the Done button is tapped and changes have been made. Navigates back to the main product
     * settings fragment and passes it a bundle containing the changes.
     */
    private fun navigateBackWithResult() {
        requireActivity().navigateBackWithResult(
                requestCode,
                getChangesBundle(),
                R.id.nav_host_fragment_main,
                R.id.productSettingsFragment
        )
    }

    /**
     * Descendants should call this when edits are made so we can show/hide the done button
     */
    fun changesMade() {
        activity?.invalidateOptionsMenu()
    }

    /**
     * Descendants should override this to return changes as a bundle
     */
    abstract fun getChangesBundle(): Bundle

    /**
     * Descendants should override this to return true if changes have been made
     */
    abstract fun hasChanges(): Boolean

    /**
     * Descendants should override this to validate any changes and return true if validation passes
     */
    abstract fun validateChanges(): Boolean
}
