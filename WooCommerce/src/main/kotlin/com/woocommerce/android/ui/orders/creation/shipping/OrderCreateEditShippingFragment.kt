package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreateEditShippingBinding
import com.woocommerce.android.extensions.drop
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreateEditShippingViewModel.RemoveShipping
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreateEditShippingViewModel.UpdateShipping
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreateEditShippingFragment :
    BaseFragment(R.layout.fragment_order_create_edit_shipping),
    MenuProvider {
    private val viewModel: OrderCreateEditShippingViewModel by viewModels()
    private val sharedViewModel: OrderCreateEditViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_creations)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        val binding = FragmentOrderCreateEditShippingBinding.bind(view)
        binding.initUi()
        setupObservers(binding)

        if (savedInstanceState == null) {
            binding.amountEditText.editText.showKeyboardWithDelay()
        }
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            viewModel.onDoneButtonClicked()
            true
        } else {
            false
        }
    }

    private fun FragmentOrderCreateEditShippingBinding.initUi() {
        amountEditText.value.filterNotNull()
            .drop(1)
            .observe(viewLifecycleOwner) {
                viewModel.onAmountEdited(it)
            }
        nameEditText.setOnTextChangedListener {
            viewModel.onNameEdited(it?.toString().orEmpty())
        }
        removeShippingButton.setOnClickListener {
            viewModel.onRemoveShippingClicked()
        }
    }

    private fun setupObservers(binding: FragmentOrderCreateEditShippingBinding) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.amount.takeIfNotEqualTo(old?.amount) { amount ->
                binding.amountEditText.setValueIfDifferent(amount)
            }
            new.name?.takeIfNotEqualTo(old?.name) { name ->
                binding.nameEditText.setTextIfDifferent(name)
            }
            new.isEditFlow.takeIfNotEqualTo(old?.isEditFlow) { isEditFlow ->
                binding.removeShippingButton.isVisible = isEditFlow
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is UpdateShipping -> {
                    sharedViewModel.onShippingEdited(event.amount, event.name)
                    findNavController().navigateUp()
                }
                is RemoveShipping -> {
                    sharedViewModel.onShippingRemoved()
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.order_creation_shipping_title_add)
    }
}
