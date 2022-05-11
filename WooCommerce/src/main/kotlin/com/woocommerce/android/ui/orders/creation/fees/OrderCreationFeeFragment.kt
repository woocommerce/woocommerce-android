package com.woocommerce.android.ui.orders.creation.fees

import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
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
import com.woocommerce.android.extensions.showKeyboardWithDelay
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.RemoveFee
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationFeeViewModel.UpdateFee
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.internal.format
import org.wordpress.android.util.ActivityUtils
import java.math.BigDecimal
import javax.inject.Inject

@AndroidEntryPoint
class OrderCreationFeeFragment :
    BaseFragment(R.layout.fragment_order_creation_fee) {
    private val sharedViewModel by hiltNavGraphViewModels<OrderCreationViewModel>(R.id.nav_graph_order_creations)
    private val editFeeViewModel by viewModels<OrderCreationFeeViewModel>()

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private var doneMenuItem: MenuItem? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        with(FragmentOrderCreationFeeBinding.bind(view)) {
            bindViews()
            observeEvents()
            observeViewStateData()

            if (savedInstanceState == null) {
                feeAmountEditText.editText.showKeyboardWithDelay()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        doneMenuItem = menu.findItem(R.id.menu_done)
        doneMenuItem?.isEnabled = editFeeViewModel.viewStateData.liveData.value?.isDoneButtonEnabled ?: false
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

    private fun styleCalculatedFee(feeAmount: BigDecimal): SpannableString {
        val formattedFeeAmount = currencyFormatter.formatCurrency(amount = feeAmount)

        val text = format(
            getString(R.string.order_creation_fee_percentage_calculated_amount),
            formattedFeeAmount
        )
        val spannable = SpannableString(text)
        spannable.setSpan(
            StyleSpan(Typeface.BOLD),
            text.length - formattedFeeAmount.length,
            text.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannable
    }

    private fun FragmentOrderCreationFeeBinding.observeViewStateData() {
        editFeeViewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.feeAmount.takeIfNotEqualTo(old?.feeAmount) { feeAmount ->
                feeAmountEditText.setValueIfDifferent(feeAmount)
                feePercentageCalculatedAmount.text = styleCalculatedFee(feeAmount)
            }
            new.feePercentage.takeIfNotEqualTo(old?.feePercentage) {
                feePercentageEditText.setTextIfDifferent(it.toPlainString())
            }
            new.isPercentageSelected.takeIfNotEqualTo(old?.isPercentageSelected) { isChecked ->
                ActivityUtils.hideKeyboard(activity)
                feePercentageEditText.isVisible = isChecked
                feePercentageCalculatedAmount.isVisible = isChecked
                feeAmountEditText.isVisible = isChecked.not()
                feeTypeSwitch.isChecked = isChecked
            }
            new.isDoneButtonEnabled.takeIfNotEqualTo(old?.isDoneButtonEnabled) {
                doneMenuItem?.isEnabled = it
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
