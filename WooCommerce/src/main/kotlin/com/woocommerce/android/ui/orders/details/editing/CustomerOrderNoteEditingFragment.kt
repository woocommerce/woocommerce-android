package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentOrderCreateEditCustomerNoteBinding
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class CustomerOrderNoteEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_order_create_edit_customer_note) {
    companion object {
        const val TAG = "EditCustomerOrderNoteFragment"
    }

    private var _binding: FragmentOrderCreateEditCustomerNoteBinding? = null
    private val binding get() = _binding!!

    private var pendingInitialNoteFill = false

    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_CUSTOMER_NOTE

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrderCreateEditCustomerNoteBinding.bind(view)
        pendingInitialNoteFill = savedInstanceState == null

        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        onPrepareMenu()

        binding.customerOrderNoteEditor.addTextChangedListener(textWatcher)
    }

    override fun onOrderLoaded() {
        if (pendingInitialNoteFill) {
            pendingInitialNoteFill = false
            with(binding.customerOrderNoteEditor) {
                setText(sharedViewModel.order.customerNote)
                setSelection(length())
                requestFocus()
                ActivityUtils.showKeyboard(this)
            }
        } else {
            updateDoneMenuItem()
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = requireActivity().getString(R.string.orderdetail_customer_provided_note)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        // Set up the toolbar menu
        binding.toolbar.inflateMenu(R.menu.menu_done)
        doneMenuItem = binding.toolbar.menu.findItem(R.id.menu_done)
        setupToolbarMenu()
    }

    private fun setupToolbarMenu() {
        binding.toolbar.setNavigationOnClickListener {
            navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.customerOrderNoteEditor.removeTextChangedListener(textWatcher)
        _binding = null
    }

    override fun getFragmentTitle() = requireActivity().getString(R.string.orderdetail_customer_provided_note)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun hasChanges() =
        sharedViewModel.isOrderLoaded && getCustomerNote() != sharedViewModel.order.customerNote

    override fun saveChanges() = sharedViewModel.updateCustomerOrderNote(getCustomerNote())

    private fun getCustomerNote() = binding.customerOrderNoteEditor.text.toString()
}
