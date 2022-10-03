package com.woocommerce.android.ui.orders.creation.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.widget.doAfterTextChanged
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditCustomerNoteBinding
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel

class OrderCreateEditCustomerNoteFragment :
    BaseFragment(R.layout.fragment_order_create_edit_customer_note),
    MenuProvider {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreateEditViewModel>(R.id.nav_graph_order_creations)

    private var _binding: FragmentOrderCreateEditCustomerNoteBinding? = null
    val binding
        get() = _binding!!

    private lateinit var doneMenuItem: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        _binding = FragmentOrderCreateEditCustomerNoteBinding.bind(view)
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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isEnabled = hasChanges()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                sharedViewModel.onCustomerNoteEdited(binding.customerOrderNoteEditor.text.toString())
                findNavController().navigateUp()
                true
            }
            else -> false
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_customer_note)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun hasChanges() =
        binding.customerOrderNoteEditor.text.toString() != sharedViewModel.currentDraft.customerNote
}
