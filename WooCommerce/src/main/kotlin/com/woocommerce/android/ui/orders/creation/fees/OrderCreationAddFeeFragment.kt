package com.woocommerce.android.ui.orders.creation.fees

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
import com.woocommerce.android.databinding.FragmentOrderCreationAddFeeBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.*
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationAddFeeFragment :
    BaseFragment(R.layout.fragment_order_creation_add_fee) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val addFeeViewModel by viewModels<OrderCreationAddFeeViewModel>()

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreationAddFeeBinding.bind(view)) {
            bindViews()
            setupObservers()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> addFeeViewModel.onDoneSelected().let { true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_fee)

    private fun FragmentOrderCreationAddFeeBinding.bindViews() {
        feeAmountEditText.initView(
            currency = sharedViewModel.currentDraft.currency,
            decimals = addFeeViewModel.currencyDecimals,
            currencyFormatter = currencyFormatter
        )
        feeAmountEditText.value.observe(viewLifecycleOwner) {
            addFeeViewModel.onFeeAmountChanged(it)
        }
        feePercentageEditText.setOnTextChangedListener {
            addFeeViewModel.onFeePercentageChanged(it?.toString().orEmpty())
        }
        feeTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            addFeeViewModel.onPercentageSwitchChanged(isChecked)
        }
    }

    private fun FragmentOrderCreationAddFeeBinding.setupObservers() {
        addFeeViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.feeAmount.takeIfNotEqualTo(old?.feeAmount) {
                feeAmountEditText.setValueIfDifferent(it)
            }
            new.feePercentage.takeIfNotEqualTo(old?.feePercentage) {
                feePercentageEditText.setTextIfDifferent(it.toString())
            }
            new.isPercentageSelected.takeIfNotEqualTo(old?.isPercentageSelected) {
                feeTypeSwitch.isChecked = it
            }
        }

        addFeeViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is UpdateFee -> {
                    sharedViewModel.onNewFeeSubmitted(event.amount, event.feeType)
                    findNavController().navigateUp()
                }
                is DisplayPercentageMode -> {
                    feeAmountEditText.isVisible = false
                    feePercentageEditText.isVisible = true
                }
                is DisplayAmountMode -> {
                    feeAmountEditText.isVisible = true
                    feePercentageEditText.isVisible = false
                }
            }
        }
    }
}
