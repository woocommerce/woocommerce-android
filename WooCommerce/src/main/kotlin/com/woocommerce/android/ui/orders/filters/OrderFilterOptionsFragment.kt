package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.extensions.toDateAtStartOfDay
import com.woocommerce.android.extensions.toEpochDay
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.OrderFilterCategoriesFragment.Companion.KEY_UPDATED_FILTER_OPTIONS
import com.woocommerce.android.ui.orders.filters.adapter.OrderFilterOptionAdapter
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnFilterOptionsSelectionUpdated
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowCustomDateRangePicker
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class OrderFilterOptionsFragment :
    BaseFragment(R.layout.fragment_order_filter_list),
    BackPressListener {

    private var _binding: FragmentOrderFilterListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: OrderFilterOptionsViewModel by viewModels()
    lateinit var orderFilterOptionAdapter: OrderFilterOptionAdapter

    @Inject
    lateinit var dateUtils: DateUtils

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            hasShadow = false,
            hasDivider = true
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderFilterListBinding.bind(view)

        setUpObservers(viewModel)
        setUpFilterOptionsRecyclerView(binding)
        binding.showOrdersButton.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
    }

    private fun setUpFilterOptionsRecyclerView(binding: FragmentOrderFilterListBinding) {
        orderFilterOptionAdapter = OrderFilterOptionAdapter(
            onFilterOptionClicked = { selectedFilterOption ->
                viewModel.onFilterOptionSelected(selectedFilterOption)
            }
        )
        binding.filterList.apply {
            layoutManager = LinearLayoutManager(requireActivity())
            adapter = orderFilterOptionAdapter
            addItemDecoration(
                DividerItemDecoration(
                    requireActivity(),
                    DividerItemDecoration.VERTICAL
                )
            )
        }
    }

    private fun setUpObservers(viewModel: OrderFilterOptionsViewModel) {
        viewModel.viewState.observe(viewLifecycleOwner) { _, newState ->
            showOrderFilterOptions(newState.filterOptions)
            requireActivity().title = newState.title
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowCustomDateRangePicker -> openDateRangePicker(event.startDay, event.endDay)
                is OnFilterOptionsSelectionUpdated -> navigateBackWithResult(
                    KEY_UPDATED_FILTER_OPTIONS,
                    event.category
                )
                is OnShowOrders -> navigateBackWithNotice(
                    OrderListFragment.FILTER_CHANGE_NOTICE_KEY,
                    R.id.orders
                )
                else -> event.isHandled = false
            }
        }
    }

    private fun openDateRangePicker(startDay: Long, endDay: Long) {
        val siteToday = dateUtils.getCurrentDateInSiteTimeZone() ?: Date()
        val selectedStart = if (startDay > 0) startDay.toDateAtStartOfDay() else siteToday
        val selectedEnd = if (startDay > 0) endDay.toDateAtStartOfDay() else siteToday
        showDateRangePicker(selectedStart, selectedEnd, siteToday) { startDate, endDate ->
            viewModel.onCustomDateRangeChanged(startDate.toEpochDay(), endDate.toEpochDay())
        }
    }

    private fun showOrderFilterOptions(orderFilterOptions: List<OrderFilterOptionUiModel>) {
        orderFilterOptionAdapter.submitList(orderFilterOptions)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()
}
