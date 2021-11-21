package com.woocommerce.android.ui.analytics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.analytics.AnalyticsContract.AnalyticsState
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeCardView
import com.woocommerce.android.ui.analytics.daterangeselector.AnalyticsDateRangeSelectorContract
import com.woocommerce.android.ui.base.TopLevelFragment
import dagger.hilt.android.AndroidEntryPoint

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
        viewModel.state.observe(viewLifecycleOwner, handleStateChange())
    }

    override fun shouldExpandToolbar(): Boolean = true

    override fun scrollToTop() {}

    override fun onDateRangeCalendarClickEvent() {
        findNavController()
                .navigateSafely(AnalyticsFragmentDirections.actionAnalyticsFragmentToDateRangeSelector(
                        requestKey = KEY_DATE_RANGE_SELECTOR_RESULT,
                        keys = getDateRangeSelectorViewState()?.availableRangeDates?.toTypedArray()
                                ?: emptyArray(),
                        values = getDateRangeSelectorViewState()?.availableRangeDates?.toTypedArray()
                                ?: emptyArray(),
                        selectedItem = getDateRangeSelectorViewState()?.defaultSelectedPeriod
                ))
    }

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

    private fun handleStateChange(): (state: AnalyticsState) -> Unit = {
        analyticsDateRangeCardView.binding.tvFromDate.text = it.analyticsDateRangeSelectorState.toDatePeriod
        analyticsDateRangeCardView.binding.tvToDate.text = it.analyticsDateRangeSelectorState.fromDatePeriod
    }

    private fun getDateRangeSelectorViewState() = viewModel.state.value?.analyticsDateRangeSelectorState
}
