package com.woocommerce.android.ui.products.variations

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

/**
 * Base class for all variations bulk update fragments.
 */
@AndroidEntryPoint
abstract class VariationsBulkUpdateBaseFragment(@LayoutRes layoutId: Int) : BaseFragment(layoutId) {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var doneMenuItem: MenuItem? = null
    private var progressDialog: CustomProgressDialog? = null

    /**
     * The view model for this fragment. A subclass of [VariationsBulkUpdateBaseViewModel].
     */
    abstract val viewModel: VariationsBulkUpdateBaseViewModel

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupMenu()
        observeEvents()
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_variations_bulk_update, menu)
                    doneMenuItem = menu.findItem(R.id.done)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    return when (item.itemId) {
                        R.id.done -> {
                            viewModel.onDoneClicked()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner
        )
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.ShowSnackbar -> {
                    ActivityUtils.hideKeyboard(requireActivity())
                    uiMessageResolver.showSnack(event.message)
                }
                is MultiLiveEvent.Event.Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * Hides and shows progress dialog.
     *
     * @param visible true to show the dialog, false to hide it.
     * @param title String resource id of the title to be shown in the dialog.
     */
    fun updateProgressbarDialogVisibility(visible: Boolean, @StringRes title: Int) {
        if (visible) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    /**
     * Enables and disables the "Done" menu item.
     *
     * @param enabled true to enable the menu item, false to disable it.
     */
    fun enableDoneButton(enabled: Boolean) {
        doneMenuItem?.isEnabled = enabled
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        hideProgressDialog()
        ActivityUtils.hideKeyboard(requireActivity())
    }
}
