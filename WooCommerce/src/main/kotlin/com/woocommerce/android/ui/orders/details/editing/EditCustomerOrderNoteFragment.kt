package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import com.woocommerce.android.databinding.FragmentOrderDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class EditCustomerOrderNoteFragment :
    BaseOrderEditFragment(R.layout.fragment_edit_customer_order_note) {
    companion object {
        const val TAG = "EditCustomerOrderNoteFragment"
        const val KEY_EDIT_NOTE_RESULT = "key_edit_note_result"
        private const val KEY_ORIGINAL_NOTE = "key_original_note"
    }

    private var _binding: FragmentEditCustomerOrderNoteBinding? = null
    private val binding get() = _binding!!

    private val navArgs: EditCustomerOrderNoteFragmentArgs by navArgs()
    private var originalNote: String = ""

    /** TODO
     * DONE button
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEditCustomerOrderNoteBinding.bind(view)

        if (savedInstanceState == null) {
            originalNote = navArgs.customerOrderNote
            binding.customerOrderNoteEditor.setText(navArgs.customerOrderNote)
            binding.customerOrderNoteEditor.requestFocus()
            ActivityUtils.showKeyboard(binding.customerOrderNoteEditor)
        } else {
            originalNote = savedInstanceState.getString(KEY_ORIGINAL_NOTE, "")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
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

    override fun hasChanges() = originalNote != getCustomerNote()

    private fun getCustomerNote() = binding.customerOrderNoteEditor.text.toString()
}
