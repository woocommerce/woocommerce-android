package com.woocommerce.android.ui.analytics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAnalyticsBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.TopLevelFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AnalyticsFragment :
    TopLevelFragment(R.layout.fragment_analytics) {
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
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { newState -> handleStateChange(newState) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun shouldExpandToolbar(): Boolean = true

    override fun scrollToTop() {
        return
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

    private fun bind(view: View) {
        _binding = FragmentAnalyticsBinding.bind(view)
        binding.analyticsDateSelectorCard.setCalendarClickListener { openDateRangeSelector() }
    }

    private fun handleStateChange(viewState: AnalyticsViewState) {
        binding.analyticsDateSelectorCard.updateFromText(viewState.analyticsDateRangeSelectorState.fromDatePeriod)
        binding.analyticsDateSelectorCard.updateToText(viewState.analyticsDateRangeSelectorState.toDatePeriod)
    }

    private fun getDateRangeSelectorViewState() = viewModel.state.value.analyticsDateRangeSelectorState
}
