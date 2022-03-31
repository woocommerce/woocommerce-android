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
import com.woocommerce.android.databinding.FragmentOrderCreationFeeBinding
import com.woocommerce.android.extensions.drop
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.RemoveFee
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.UpdateFee
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationFeeFragment :
    BaseFragment(R.layout.fragment_order_creation_fee) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val editFeeViewModel by viewModels<OrderCreationFeeViewModel>()

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreationFeeBinding.bind(view)) {
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

    private fun FragmentOrderCreationFeeBinding.bindViews() {
        feeAmountEditText.value.filterNotNull().drop(1).observe(viewLifecycleOwner) {
            editFeeViewModel.onFeeAmountChanged(it)
        }
        feePercentageEditText.setOnTextChangedListener {
            editFeeViewModel.onFeePercentageChanged(it?.toString().orEmpty())
        }
        feeTypeSwitch.setOnCheckedChangeListener { _, isChecked ->
            editFeeViewModel.onPercentageSwitchChanged(isChecked)
        }
        removeFeeButton.setOnClickListener {
            editFeeViewModel.onRemoveFeeClicked()
        }
    }

    private fun FragmentOrderCreationFeeBinding.observeViewStateData() {
        editFeeViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.feeAmount.takeIfNotEqualTo(old?.feeAmount) { feeAmount ->
                feeAmountEditText.setValueIfDifferent(feeAmount)
                val formattedAmount = currencyFormatter.formatCurrency(amount = feeAmount)
                feePercentageEditText.suffixText = formattedAmount
            }
            new.feePercentage.takeIfNotEqualTo(old?.feePercentage) {
                feePercentageEditText.setTextIfDifferent(it.toPlainString())
            }
            new.isPercentageSelected.takeIfNotEqualTo(old?.isPercentageSelected) { isChecked ->
                ActivityUtils.hideKeyboard(activity)
                feePercentageEditText.isVisible = isChecked
                feeAmountEditText.isVisible = isChecked.not()
                feeTypeSwitch.isChecked = isChecked
            }
            new.shouldDisplayRemoveFeeButton.takeIfNotEqualTo(old?.shouldDisplayRemoveFeeButton) {
                removeFeeButton.isVisible = it
            }
            new.shouldDisplayPercentageSwitch.takeIfNotEqualTo(old?.shouldDisplayPercentageSwitch) {
                feeTypeSwitch.isVisible = it
            }
        }
    }

    private fun observeEvents() {
        editFeeViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is UpdateFee -> sharedViewModel.onFeeEdited(event.amount)
                is RemoveFee -> sharedViewModel.onFeeRemoved()
            }
            findNavController().navigateUp()
        }
    }
}
