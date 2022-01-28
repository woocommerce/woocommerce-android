package com.woocommerce.android.ui.orders.creation.notes

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.widget.doAfterTextChanged
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentEditCustomerOrderNoteBinding
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel

class OrderCreationCustomerNoteFragment : BaseFragment(R.layout.fragment_edit_customer_order_note) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)

    private var _binding: FragmentEditCustomerOrderNoteBinding? = null
    val binding
        get() = _binding!!

    private lateinit var doneMenuItem: MenuItem

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _binding = FragmentEditCustomerOrderNoteBinding.bind(view)
        if (savedInstanceState == null) {
            binding.customerOrderNoteEditor.setText(sharedViewModel.currentDraft.customerNote)
        }
        binding.customerOrderNoteEditor.doAfterTextChanged {
            if (::doneMenuItem.isInitialized) {
                doneMenuItem.isVisible = hasChanges()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem.isVisible = hasChanges()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                sharedViewModel.onCustomerNoteEdited(binding.customerOrderNoteEditor.text.toString())
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
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
