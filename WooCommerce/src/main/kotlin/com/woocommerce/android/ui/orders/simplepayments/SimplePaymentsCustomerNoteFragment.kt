package com.woocommerce.android.ui.orders.simplepayments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils

class SimplePaymentsCustomerNoteFragment : BaseFragment(R.layout.fragment_edit_customer_order_note) {
    private var _binding: FragmentEditCustomerOrderNoteBinding? = null
    val binding
        get() = _binding!!

    private val navArgs: SimplePaymentsCustomerNoteFragmentArgs by navArgs()
    private lateinit var doneMenuItem: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _binding = FragmentEditCustomerOrderNoteBinding.bind(view)
        if (savedInstanceState == null) {
            binding.customerOrderNoteEditor.setText(navArgs.customerNote)
            if (binding.customerOrderNoteEditor.requestFocus() && !DisplayUtils.isLandscape(requireActivity())) {
                binding.customerOrderNoteEditor.postDelayed(
                    {
                        ActivityUtils.showKeyboard(binding.customerOrderNoteEditor)
                    },
                    KEYBOARD_DELAY
                )
            }
        }
        binding.customerOrderNoteEditor.doAfterTextChanged {
            if (::doneMenuItem.isInitialized) {
                doneMenuItem.isEnabled = hasChanges()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isEnabled = hasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                navigateBackWithResult(
                    SIMPLE_PAYMENTS_CUSTOMER_NOTE_RESULT,
                    binding.customerOrderNoteEditor.text.toString()
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.orderdetail_customer_provided_note)

    private fun hasChanges() =
        binding.customerOrderNoteEditor.text.toString() != navArgs.customerNote

    companion object {
        const val SIMPLE_PAYMENTS_CUSTOMER_NOTE_RESULT = "simple_payments_customer_note_result"
        private const val KEYBOARD_DELAY = 100L
    }
}
