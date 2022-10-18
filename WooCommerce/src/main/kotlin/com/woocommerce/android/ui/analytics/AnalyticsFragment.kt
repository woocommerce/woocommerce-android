package com.woocommerce.android.ui.analytics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAnalyticsBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.RefreshIndicator.*
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticsFragment :
    BaseFragment(R.layout.fragment_analytics) {
    companion object {
        const val KEY_DATE_RANGE_SELECTOR_RESULT = "key_order_status_result"
    }

    private val viewModel: AnalyticsViewModel by viewModels()
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding
        get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.analytics)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(view)
        setupResultHandlers(viewModel)
        lifecycleScope.launchWhenStarted { viewModel.state.collect { newState -> handleStateChange(newState) } }
        viewModel.event.observe(viewLifecycleOwner, { event -> handleEvent(event) })
        binding.analyticsRefreshLayout.setOnRefreshListener {
            binding.analyticsRefreshLayout.scrollUpChild = binding.scrollView
            viewModel.onRefreshRequested()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun handleEvent(event: MultiLiveEvent.Event) {
        when (event) {
            is AnalyticsViewEvent.OpenUrl -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
            is AnalyticsViewEvent.OpenWPComWebView -> findNavController()
                .navigate(NavGraphMainDirections.actionGlobalWPComWebViewFragment(urlToLoad = event.url))
            else -> event.isHandled = false
        }
    }

    private fun openDateRangeSelector() = findNavController().navigateSafely(buildDialogDateRangeSelectorArguments())

    private fun buildDialogDateRangeSelectorArguments() =
        AnalyticsFragmentDirections.actionAnalyticsFragmentToDateRangeSelector(
            requestKey = KEY_DATE_RANGE_SELECTOR_RESULT,
            keys = getDateRangeSelectorViewState().availableRangeDates.toTypedArray(),
            values = getDateRangeSelectorViewState().availableRangeDates.toTypedArray(),
            selectedItem = getDateRangeSelectorViewState().selectedPeriod
        )

    private fun setupResultHandlers(viewModel: AnalyticsViewModel) {
        handleDialogResult<String>(
            key = KEY_DATE_RANGE_SELECTOR_RESULT,
            entryId = R.id.analytics
        ) { dateRange -> viewModel.onSelectedTimePeriodChanged(dateRange) }
    }

    private fun bind(view: View) {
        _binding = FragmentAnalyticsBinding.bind(view)
        binding.analyticsDateSelectorCard.setCalendarClickListener { openDateRangeSelector() }
        binding.analyticsRevenueCard.setSeeReportClickListener { viewModel.onRevenueSeeReportClick() }
        binding.analyticsOrdersCard.setSeeReportClickListener { viewModel.onOrdersSeeReportClick() }
        binding.analyticsProductsCard.setSeeReportClickListener { viewModel.onProductsSeeReportClick() }
    }

    private fun handleStateChange(viewState: AnalyticsViewState) {
        binding.analyticsDateSelectorCard.updateFromText(viewState.analyticsDateRangeSelectorState.fromDatePeriod)
        binding.analyticsDateSelectorCard.updateToText(viewState.analyticsDateRangeSelectorState.toDatePeriod)
        binding.analyticsRevenueCard.updateInformation(viewState.revenueState)
        binding.analyticsOrdersCard.updateInformation(viewState.ordersState)
        binding.analyticsProductsCard.updateInformation(viewState.productsState)
        binding.analyticsRefreshLayout.isRefreshing = viewState.refreshIndicator == ShowIndicator
    }

    private fun getDateRangeSelectorViewState() = viewModel.state.value.analyticsDateRangeSelectorState
}
