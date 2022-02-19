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
import com.woocommerce.android.databinding.FragmentOrderCreationEditFeeBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationEditFeeViewModel.UpdateFee
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationEditFeeFragment :
    BaseFragment(R.layout.fragment_order_creation_edit_fee) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val editFeeViewModel by viewModels<OrderCreationEditFeeViewModel>()

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreationEditFeeBinding.bind(view)) {
            bindViews()
            observeEvents()
            observeViewStateData()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> editFeeViewModel.onDoneSelected().let { true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getFragmentTitle() = getString(R.string.order_creation_add_fee)

    private fun FragmentOrderCreationEditFeeBinding.bindViews() {
        feeAmountEditText.initView(
            currency = sharedViewModel.currentDraft.currency,
            decimals = editFeeViewModel.currencyDecimals,
            currencyFormatter = currencyFormatter
        )
        feeAmountEditText.value.observe(viewLifecycleOwner) {
            editFeeViewModel.onFeeAmountChanged(it)
        }
        feePercentageEditText.setOnTextChangedListener {
            editFeeViewModel.onFeePercentageChanged(it?.toString().orEmpty())
        }
        feeTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            editFeeViewModel.onPercentageSwitchChanged(isChecked)
        }
    }

    private fun FragmentOrderCreationEditFeeBinding.observeViewStateData() {
        editFeeViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.feeAmount.takeIfNotEqualTo(old?.feeAmount) {
                feeAmountEditText.setValueIfDifferent(it)
            }
            new.feePercentage.takeIfNotEqualTo(old?.feePercentage) {
                feePercentageEditText.setTextIfDifferent(it.toString())
            }
            new.isPercentageSelected.takeIfNotEqualTo(old?.isPercentageSelected) { isChecked ->
                ActivityUtils.hideKeyboard(activity)
                feePercentageEditText.isVisible = isChecked
                feeAmountEditText.isVisible = isChecked.not()
                feeTypeSwitch.isChecked = isChecked
            }
        }
    }

    private fun observeEvents() {
        editFeeViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is UpdateFee -> {
                    sharedViewModel.onFeeEdited(event.amount, event.feeType)
                    findNavController().navigateUp()
                }
            }
        }
    }
}
