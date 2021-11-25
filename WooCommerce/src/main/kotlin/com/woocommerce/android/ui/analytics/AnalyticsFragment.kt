package com.woocommerce.android.ui.analytics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.AnalyticsContract.AnalyticsState
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCardView
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract
import com.woocommerce.android.ui.base.TopLevelFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class AnalyticsFragment :
    TopLevelFragment(R.layout.fragment_analytics),
    AnalyticsDateRangeSelectorContract.DateRangeEvent {
    companion object {
        const val KEY_DATE_RANGE_SELECTOR_RESULT = "key_order_status_result"
    }

    private lateinit var analyticsDateRangeCardView: AnalyticsDateRangeCardView
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun getFragmentTitle() = getString(R.string.analytics)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bind(view)
        setupResultHandlers(viewModel)
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { newState -> handleStateChange(newState) }
        }
    }

    override fun shouldExpandToolbar(): Boolean = true

    override fun scrollToTop() {
        return
    }

    override fun onDateRangeCalendarClickEvent() {
        openDateRangeSelector()
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
        ) { dateRange -> viewModel.onSelectedDateRangeChanged(dateRange) }
    }

    private fun bind(rootView: View) {
        analyticsDateRangeCardView = rootView.findViewById(R.id.analytics_date_selector_card)
        analyticsDateRangeCardView.initView(this)
    }

    private fun handleStateChange(state: AnalyticsState) {
        analyticsDateRangeCardView.binding.tvFromDate.text = state.analyticsDateRangeSelectorState.fromDatePeriod
        analyticsDateRangeCardView.binding.tvToDate.text = state.analyticsDateRangeSelectorState.toDatePeriod
    }

    private fun getDateRangeSelectorViewState() = viewModel.state.value.analyticsDateRangeSelectorState
}
