package com.woocommerce.android.ui.orders.creation.shipping

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderCreationShippingBinding
import com.woocommerce.android.extensions.drop
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreationShippingViewModel.RemoveShipping
import com.woocommerce.android.ui.orders.creation.shipping.OrderCreationShippingViewModel.UpdateShipping
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationShippingFragment : BaseFragment(R.layout.fragment_order_creation_shipping) {
    private val viewModel: OrderCreationShippingViewModel by viewModels()
    private val sharedViewModel: OrderCreationViewModel by hiltNavGraphViewModels(R.id.nav_graph_order_creations)

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        val binding = FragmentOrderCreationShippingBinding.bind(view)
        binding.initUi()
        setupObservers(binding)

        if (savedInstanceState == null) {
            binding.amountEditText.editText.showKeyboardWithDelay()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.menu_done) {
            viewModel.onDoneButtonClicked()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun FragmentOrderCreationShippingBinding.initUi() {
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

    private fun setupObservers(binding: FragmentOrderCreationShippingBinding) {
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
