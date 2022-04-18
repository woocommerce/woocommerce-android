package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class CustomerOrderNoteEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_edit_customer_order_note) {
    companion object {
        const val TAG = "EditCustomerOrderNoteFragment"
    }

    private var _binding: FragmentEditCustomerOrderNoteBinding? = null
    private val binding get() = _binding!!

    override val analyticsValue: String = AnalyticsTracker.ORDER_EDIT_CUSTOMER_NOTE

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = R.drawable.ic_gridicons_cross_24dp
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditCustomerOrderNoteBinding.bind(view)

        if (savedInstanceState == null) {
            binding.customerOrderNoteEditor.setText(sharedViewModel.order.customerNote)
            binding.customerOrderNoteEditor.requestFocus()
            ActivityUtils.showKeyboard(binding.customerOrderNoteEditor)
        }

        binding.customerOrderNoteEditor.addTextChangedListener(textWatcher)
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

    override fun hasChanges() = getCustomerNote() != sharedViewModel.order.customerNote

    override fun saveChanges() = sharedViewModel.updateCustomerOrderNote(getCustomerNote())

    private fun getCustomerNote() = binding.customerOrderNoteEditor.text.toString()
}
