package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import androidx.navigation.navGraphViewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.CustomDiscardDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

/**
 * All product related fragments should extend this class to provide a consistent method
 * of displaying snackbar, handling navigation and discard dialogs
 */
abstract class BaseProductFragment : BaseFragment(), BackPressListener {
    @Inject lateinit var navigator: ProductNavigator
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var viewModelFactory: ViewModelFactory

    protected val viewModel: ProductDetailViewModel by navGraphViewModels(R.id.nav_graph_products) { viewModelFactory }

    private var doneOrUpdateMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)

        // if this is the initial creation of this fragment, tell the viewModel to make a copy of the product
        // as it exists now so we can easily discard changes are determine if any changes were made inside
        // this fragment
        if (savedInstanceState == null) {
            viewModel.updateProductBeforeEnteringFragment()
        }
    }

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> requireActivity().onBackPressed()
                is ShowDiscardDialog -> CustomDiscardDialog.showDiscardDialog(
                        requireActivity(),
                        event.positiveBtnAction,
                        event.negativeBtnAction,
                        event.messageId
                )
                is ProductNavigationTarget -> navigator.navigate(this, event)
                else -> event.isHandled = false
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        doneOrUpdateMenuItem = menu.findItem(R.id.menu_done)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        showUpdateMenuItem(hasChanges())
    }

    protected fun enableUpdateMenuItem(enable: Boolean) {
        doneOrUpdateMenuItem?.isEnabled = enable
    }

    protected fun showUpdateMenuItem(show: Boolean) {
        doneOrUpdateMenuItem?.isVisible = show
    }

    override fun onStop() {
        super.onStop()
        CustomDiscardDialog.onCleared()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    /**
     * Determines if changes have been made in the active fragment
     */
    open fun hasChanges(): Boolean {
        return viewModel.getProduct().productBeforeEnteringFragment?.let {
            viewModel.getProduct().productDraft?.isSameProduct(it) == false
        } ?: false
    }

    /**
     * Descendants should call this when edits are made so we can show/hide the done/publish button
     */
    protected fun changesMade() {
        showUpdateMenuItem(hasChanges())
    }
}
