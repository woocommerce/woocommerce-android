package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.MaterialDatePicker
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterCustomDateRangeBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.OrderFilterOptionsFragment.Companion.ON_DATE_RANGE_CHANGE_KEY
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnDateRangeChanged
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.list.OrderListFragment.Companion.FILTER_CHANGE_NOTICE_KEY
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderFilterCustomDateRangeFragment :
    BaseFragment(R.layout.fragment_order_filter_custom_date_range),
    BackPressListener {
    private companion object {
        const val DATE_PICKER_FRAGMENT_TAG = "DateRangePicker"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    private val viewModel: OrderFilterCustomDateRangeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        requireActivity().title = getString(R.string.orderfilters_date_range_filter_custom_range)
        val binding = FragmentOrderFilterCustomDateRangeBinding.bind(view)
        setUpObservers(viewModel, binding)

        binding.showOrdersButton.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()

    private fun setUpObservers(
        viewModel: OrderFilterCustomDateRangeViewModel,
        binding: FragmentOrderFilterCustomDateRangeBinding
    ) {
        viewModel.viewState.observe(viewLifecycleOwner) { _, newState ->
            binding.startDateValueTextView.text = newState.startDateDisplayValue
            binding.endDateValueTextView.text = newState.endDateDisplayValue

            val selectedStartMillis = newState.startDateMillis ?: System.currentTimeMillis()
            val selectedEndMillis = newState.endDateMillis ?: System.currentTimeMillis()

            binding.startDateLayout.setOnClickListener {
                showDateRangePicker(selectedStartMillis, selectedEndMillis) {
                    viewModel.onDateRangeSelected(it.first, it.second)
                }
            }
            binding.endDateLayout.setOnClickListener {
                showDateRangePicker(selectedStartMillis, selectedEndMillis) {
                    viewModel.onDateRangeSelected(it.first, it.second)
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnDateRangeChanged -> navigateBackWithResult(
                    ON_DATE_RANGE_CHANGE_KEY,
                    event.dateRangeDisplayValue
                )
                is OnShowOrders -> navigateBackWithNotice(
                    FILTER_CHANGE_NOTICE_KEY,
                    R.id.orders
                )
                else -> event.isHandled = false
            }
        }
    }

    private fun showDateRangePicker(
        selectedStartMillis: Long,
        selectedEndMillis: Long,
        onNewDateSelected: (rangeMillis: Pair<Long, Long>) -> Unit
    ) {
        val datePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.orderfilters_date_range_picker_title))
                .setSelection(androidx.core.util.Pair(selectedStartMillis, selectedEndMillis))
                .build()
        datePicker.show(parentFragmentManager, DATE_PICKER_FRAGMENT_TAG)
        datePicker.addOnPositiveButtonClickListener {
            onNewDateSelected.invoke(Pair(it.first, it.second))
        }
    }
}
