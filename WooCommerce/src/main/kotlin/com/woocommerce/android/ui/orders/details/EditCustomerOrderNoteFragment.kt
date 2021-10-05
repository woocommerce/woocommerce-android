package com.woocommerce.android.ui.orders.details

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils

@AndroidEntryPoint
class EditCustomerOrderNoteFragment : BaseFragment(R.layout.fragment_edit_customer_order_note), BackPressListener {
    companion object {
        const val TAG = "EditCustomerOrderNoteFragment"
        const val KEY_EDIT_NOTE_RESULT = "key_edit_note_result"
    }

    private val navArgs: EditCustomerOrderNoteFragmentArgs by navArgs()

    /** TODO
     * cross icon
     * DONE button
     * discard dialog
     * make text selectable in order detail when feature flag not enabled
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentEditCustomerOrderNoteBinding.bind(view)

        if (savedInstanceState == null) {
            binding.customerOrderNoteEditor.setText(navArgs.customerOrderNote)
            binding.customerOrderNoteEditor.requestFocus()
            ActivityUtils.showKeyboard(binding.customerOrderNoteEditor)
        }
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

    override fun onRequestAllowBackPress() = true
}
