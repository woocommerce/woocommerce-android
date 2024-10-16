package com.woocommerce.android.ui.payments.simplepayments

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentOrderCreateEditCustomerNoteBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import org.wordpress.android.util.ActivityUtils
import org.wordpress.android.util.DisplayUtils

class SimplePaymentsCustomerNoteFragment :
    BaseFragment(R.layout.fragment_order_create_edit_customer_note) {
    private var _binding: FragmentOrderCreateEditCustomerNoteBinding? = null
    val binding
        get() = _binding!!

    private val navArgs: SimplePaymentsCustomerNoteFragmentArgs by navArgs()
    private lateinit var doneMenuItem: MenuItem

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderCreateEditCustomerNoteBinding.bind(view)
        setupToolbar()
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

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.orderdetail_customer_provided_note)
        onCreateMenu(_binding)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
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

    private fun onCreateMenu(binding: FragmentOrderCreateEditCustomerNoteBinding?) {
        binding?.toolbar?.inflateMenu(R.menu.menu_done)
        doneMenuItem = binding?.toolbar?.menu?.findItem(R.id.menu_done)!!
        doneMenuItem.isEnabled = hasChanges()
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                navigateBackWithResult(
                    SIMPLE_PAYMENTS_CUSTOMER_NOTE_RESULT,
                    binding.customerOrderNoteEditor.text.toString()
                )
                true
            }
            else -> false
        }
    }

    private fun hasChanges() =
        binding.customerOrderNoteEditor.text.toString() != navArgs.customerNote

    companion object {
        const val SIMPLE_PAYMENTS_CUSTOMER_NOTE_RESULT = "simple_payments_customer_note_result"
        private const val KEYBOARD_DELAY = 100L
    }
}
