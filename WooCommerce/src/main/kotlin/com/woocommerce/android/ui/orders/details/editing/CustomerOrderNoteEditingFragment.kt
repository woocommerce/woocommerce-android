package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class CustomerOrderNoteEditingFragment :
    BaseOrderEditingFragment(R.layout.fragment_edit_customer_order_note) {
    companion object {
        const val TAG = "EditCustomerOrderNoteFragment"
        private const val KEY_ORIGINAL_NOTE = "original_note"
    }

    private var _binding: FragmentEditCustomerOrderNoteBinding? = null
    private val binding get() = _binding!!

    private var originalNote: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditCustomerOrderNoteBinding.bind(view)

        if (savedInstanceState == null) {
            originalNote = sharedViewModel.customerOrderNote
            binding.customerOrderNoteEditor.setText(originalNote)
            binding.customerOrderNoteEditor.requestFocus()
            ActivityUtils.showKeyboard(binding.customerOrderNoteEditor)
        } else {
            originalNote = savedInstanceState.getString(KEY_ORIGINAL_NOTE) ?: ""
        }

        binding.customerOrderNoteEditor.addTextChangedListener(textWatcher)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.customerOrderNoteEditor.removeTextChangedListener(textWatcher)
        _binding = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_ORIGINAL_NOTE, originalNote)
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

    override fun hasChanges() = getCustomerNote() != originalNote

    override fun saveChanges() = sharedViewModel.updateCustomerOrderNote(getCustomerNote())

    private fun getCustomerNote() = binding.customerOrderNoteEditor.text.toString()
}
