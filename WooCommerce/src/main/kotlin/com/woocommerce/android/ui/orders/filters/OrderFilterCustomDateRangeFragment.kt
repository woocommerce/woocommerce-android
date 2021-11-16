package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterCustomDateRangeBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnDateRangeChanged
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.list.OrderListFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderFilterCustomDateRangeFragment :
    BaseFragment(R.layout.fragment_order_filter_custom_date_range),
    BackPressListener {
    private companion object {
        const val DATE_PICKER_FRAGMENT_TAG = "DateRangePicker"
    }

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
            binding.startDateLayout.setOnClickListener {
                showDateRangePicker(newState.startDateMillis) {
                    viewModel.onStartDateSelected(it)
                }
            }
            binding.endDateLayout.setOnClickListener {
                showDateRangePicker(newState.endDateMillis) {
                    viewModel.onEndDateSelected(it)
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OnDateRangeChanged -> findNavController().navigateUp()
                is OnShowOrders -> navigateBackWithNotice(
                    OrderListFragment.FILTER_CHANGE_NOTICE_KEY,
                    R.id.orders
                )
                else -> event.isHandled = false
            }
        }
    }

    private fun showDateRangePicker(
        selectedDate: Long,
        onNewDateSelected: (dateMillis: Long) -> Unit
    ) {
        val datePicker =
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.orderfilters_date_range_picker_title))
                .setSelection(selectedDate)
                .build()
        datePicker.show(parentFragmentManager, DATE_PICKER_FRAGMENT_TAG)
        datePicker.addOnPositiveButtonClickListener {
            onNewDateSelected.invoke(it)
        }
    }
}
