package com.woocommerce.android.ui.analytics.hub

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAnalyticsBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class AnalyticsHubFragment :
    BaseFragment(R.layout.fragment_analytics) {
    companion object {
        const val KEY_DATE_RANGE_SELECTOR_RESULT = "key_order_status_result"
        const val DATE_PICKER_FRAGMENT_TAG = "DateRangePicker"
    }

    private val viewModel: AnalyticsHubViewModel by viewModels()
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding
        get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.analytics)

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(viewModel.performanceObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(view)
        setupResultHandlers(viewModel)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.flowWithLifecycle(lifecycle).collect { newState -> handleStateChange(newState) }
        }

        viewModel.event.observe(viewLifecycleOwner) { event -> handleEvent(event) }
        binding.analyticsRefreshLayout.setOnRefreshListener {
            binding.analyticsRefreshLayout.scrollUpChild = binding.scrollView
            viewModel.onTrackableUIInteraction()
            viewModel.onRefreshRequested()
        }
        binding.scrollView.scrollStartEvents()
            .onEach { viewModel.onTrackableUIInteraction() }
            .launchIn(viewLifecycleOwner.lifecycleScope)
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
            is AnalyticsViewEvent.OpenDatePicker -> showDateRangePicker(event.fromMillis, event.toMillis)
            is AnalyticsViewEvent.OpenDateRangeSelector -> openDateRangeSelector()
            is AnalyticsViewEvent.SendFeedback -> sendFeedback()
            else -> event.isHandled = false
        }
    }

    private fun openDateRangeSelector() = findNavController().navigateSafely(buildDialogDateRangeSelectorArguments())

    private fun buildDialogDateRangeSelectorArguments() =
        AnalyticsHubFragmentDirections.actionAnalyticsFragmentToDateRangeSelector(
            requestKey = KEY_DATE_RANGE_SELECTOR_RESULT,
            keys = viewModel.selectableRangeOptions,
            values = StatsTimeRangeSelection.SelectionType.names,
            selectedItem = getDateRangeSelectorViewState().selectionType.name
        )

    private fun setupResultHandlers(viewModel: AnalyticsHubViewModel) {
        handleDialogResult<String>(
            key = KEY_DATE_RANGE_SELECTOR_RESULT,
            entryId = R.id.analytics
        ) { dateSelection ->
            StatsTimeRangeSelection.SelectionType.from(dateSelection)
                .takeIf { it != CUSTOM }
                ?.let { viewModel.onNewRangeSelection(it) }
                ?: viewModel.onCustomDateRangeClicked()
        }
    }

    private fun bind(view: View) {
        _binding = FragmentAnalyticsBinding.bind(view)
        binding.analyticsDateSelectorCard.setOnClickListener { viewModel.onDateRangeSelectorClick() }
    }

    private fun handleStateChange(viewState: AnalyticsViewState) {
        binding.analyticsDateSelectorCard.updateSelectionTitle(viewState.analyticsDateRangeSelectorState.selectionType)
        binding.analyticsDateSelectorCard.updatePreviousRange(viewState.analyticsDateRangeSelectorState.previousRange)
        binding.analyticsDateSelectorCard.updateCurrentRange(viewState.analyticsDateRangeSelectorState.currentRange)
        binding.analyticsDateSelectorCard.updateLastUpdateTimestamp(viewState.lastUpdateTimestamp)
        binding.analyticsRevenueCard.updateInformation(viewState.revenueState)
        binding.analyticsOrdersCard.updateInformation(viewState.ordersState)
        binding.analyticsProductsCard.updateInformation(viewState.productsState)
        binding.analyticsVisitorsCard.updateInformation(viewState.sessionState)
        binding.analyticsRefreshLayout.isRefreshing = viewState.refreshIndicator == ShowIndicator
        displayFeedbackBanner(viewState.showFeedBackBanner)
    }

    private fun getDateRangeSelectorViewState() = viewModel.viewState.value.analyticsDateRangeSelectorState

    private fun showDateRangePicker(fromMillis: Long, toMillis: Long) {
        val datePicker =
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(getString(R.string.orderfilters_date_range_picker_title))
                .setSelection(androidx.core.util.Pair(fromMillis, toMillis))
                .setCalendarConstraints(
                    CalendarConstraints.Builder()
                        .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
                        .build()
                )
                .build()
        datePicker.show(parentFragmentManager, DATE_PICKER_FRAGMENT_TAG)
        datePicker.addOnPositiveButtonClickListener {
            viewModel.onCustomRangeSelected(Date(it?.first ?: 0L), Date(it.second ?: 0L))
        }
    }

    private fun displayFeedbackBanner(isVisible: Boolean) {
        binding.analyticsHubFeedbackBanner.isVisible = isVisible
        if (!isVisible) return
        binding.analyticsHubFeedbackBanner.run {
            onSendFeedbackListener = { viewModel.onSendFeedbackClicked() }
            onDismissClickListener = { viewModel.onDismissBannerClicked() }
        }
    }

    private fun sendFeedback() {
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.ANALYTICS_HUB)
            .apply { findNavController().navigateSafely(this) }
    }
}
