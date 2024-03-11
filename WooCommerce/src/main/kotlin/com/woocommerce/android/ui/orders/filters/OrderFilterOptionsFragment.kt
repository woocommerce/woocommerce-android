package com.woocommerce.android.ui.orders.filters

import android.os.Bundle
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderFilterListBinding
import com.woocommerce.android.extensions.isTablet
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.orders.filters.OrderFilterCategoriesFragment.Companion.KEY_UPDATED_FILTER_OPTIONS
import com.woocommerce.android.ui.orders.filters.adapter.OrderFilterOptionAdapter
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnFilterOptionsSelectionUpdated
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowCustomDateRangePicker
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.list.OrderListFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils

@AndroidEntryPoint
class OrderFilterOptionsFragment :
    DialogFragment(R.layout.fragment_order_filter_list),
    BackPressListener {

    private var _binding: FragmentOrderFilterListBinding? = null
    private val binding get() = _binding!!
    private companion object {
        const val DATE_PICKER_FRAGMENT_TAG = "DateRangePicker"
        const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.55f
        const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.6f
    }

    private val viewModel: OrderFilterOptionsViewModel by viewModels()
    lateinit var orderFilterOptionAdapter: OrderFilterOptionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isTablet()) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog_RoundedCorners_NoMinWidth)
        } else {
            /* This draws the dialog as full screen */
            setStyle(STYLE_NO_TITLE, R.style.Theme_Woo)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentOrderFilterListBinding.bind(view)
        setupToolbar(binding)
        setUpObservers(viewModel)
        setUpFilterOptionsRecyclerView(binding)
        binding.showOrdersButton.setOnClickListener {
            viewModel.onShowOrdersClicked()
        }
    }

    override fun onStart() {
        super.onStart()
        if (isTablet()) {
            dialog?.window?.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun setupToolbar(binding: FragmentOrderFilterListBinding) {
        binding.toolbar.navigationIcon = AppCompatResources.getDrawable(
            requireActivity(),
            R.drawable.ic_back_24dp
        )
        binding.toolbar.setNavigationOnClickListener {
            onRequestAllowBackPress()
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
            binding.toolbar.title = newState.title
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowCustomDateRangePicker -> openDateRangePicker(event.startDateMillis, event.endDateMillis)
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

    private fun openDateRangePicker(startDateMillis: Long, endDateMillis: Long) {
        val selectedStartMillis = when {
            startDateMillis > 0 -> startDateMillis
            else -> System.currentTimeMillis()
        }
        val selectedEndMillis = when {
            startDateMillis > 0 -> endDateMillis
            else -> System.currentTimeMillis()
        }
        showDateRangePicker(selectedStartMillis, selectedEndMillis) { start, end ->
            viewModel.onCustomDateRangeChanged(start, end)
        }
    }

    private fun showOrderFilterOptions(orderFilterOptions: List<OrderFilterOptionUiModel>) {
        orderFilterOptionAdapter.submitList(orderFilterOptions)
    }

    override fun onRequestAllowBackPress() = viewModel.onBackPressed()
}
