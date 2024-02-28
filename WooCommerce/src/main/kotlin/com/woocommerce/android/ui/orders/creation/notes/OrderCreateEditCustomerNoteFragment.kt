package com.woocommerce.android.ui.orders.creation.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditCustomerNoteBinding
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels

class OrderCreateEditCustomerNoteFragment :
    BaseFragment(R.layout.fragment_order_create_edit_customer_note) {
    private val sharedViewModel by fixedHiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

    private var _binding: FragmentOrderCreateEditCustomerNoteBinding? = null
    val binding
        get() = _binding!!

    private lateinit var doneMenuItem: MenuItem

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderCreateEditCustomerNoteBinding.bind(view)
        setupToolbar()
        if (savedInstanceState == null) {
            binding.customerOrderNoteEditor.setText(sharedViewModel.currentDraft.customerNote)
            binding.customerOrderNoteEditor.showKeyboardWithDelay()
        }
        binding.customerOrderNoteEditor.doAfterTextChanged {
            if (::doneMenuItem.isInitialized) {
                doneMenuItem.isEnabled = hasChanges()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.title = getString(R.string.order_creation_customer_note)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            onMenuItemSelected(menuItem)
        }
        // Set up the toolbar menu
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        setupToolbarMenu(binding.toolbar.menu)
    }

    private fun setupToolbarMenu(menu: Menu) {
        binding.toolbar.inflateMenu(R.menu.menu_done)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isEnabled = hasChanges()
    }

    private fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                sharedViewModel.onCustomerNoteEdited(binding.customerOrderNoteEditor.text.toString())
                findNavController().navigateUp()
                true
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hasChanges() =
        binding.customerOrderNoteEditor.text.toString() != sharedViewModel.currentDraft.customerNote
}
