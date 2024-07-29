package com.woocommerce.android.ui.analytics.hub

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentAnalyticsBinding
import com.woocommerce.android.extensions.handleDialogResult
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.ui.analytics.hub.RefreshIndicator.ShowIndicator
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.common.MarginBottomItemDecoration
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewFragment.Companion.WEBVIEW_RESULT
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.EntryPointSource.ANALYTICS_HUB
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel.UrlComparisonMode.PARTIAL
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date

@AndroidEntryPoint
class AnalyticsHubFragment : BaseFragment(R.layout.fragment_analytics) {
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
        setupMenu()

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
            is AnalyticsViewEvent.OpenGoogleAdsCreation -> startGoogleAdsWebView(
                url = event.url,
                title = event.title,
                isCreationFlow = event.isCreationFlow
            )

            is AnalyticsViewEvent.OpenUrl -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)

            is AnalyticsViewEvent.OpenWPComWebView -> findNavController()
                .navigate(NavGraphMainDirections.actionGlobalWPComWebViewFragment(urlToLoad = event.url))

            is AnalyticsViewEvent.OpenDatePicker -> showDateRangePicker(
                event.fromMillis,
                event.toMillis
            ) { start, end ->
                viewModel.onCustomRangeSelected(Date(start), Date(end))
            }

            is AnalyticsViewEvent.OpenDateRangeSelector -> openDateRangeSelector()

            is AnalyticsViewEvent.SendFeedback -> sendFeedback()

            is AnalyticsViewEvent.OpenSettings -> findNavController()
                .navigateSafely(AnalyticsHubFragmentDirections.actionAnalyticsToAnalyticsSettings())

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

        handleNotice(WEBVIEW_RESULT) {
            findNavController().navigateSafely(
                NavGraphMainDirections.actionGlobalGoogleAdsCampaignSuccessBottomSheet()
            )
        }
    }

    private fun bind(view: View) {
        _binding = FragmentAnalyticsBinding.bind(view)
        binding.analyticsDateSelectorCard.setOnClickListener { viewModel.onDateRangeSelectorClick() }
        val cardsAdapter = AnalyticsHubCardsAdapter().apply {
            onSeeReport = viewModel::onSeeReport
        }
        binding.cards.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = cardsAdapter
            isNestedScrollingEnabled = false
            addItemDecoration(MarginBottomItemDecoration(R.dimen.major_100, requireContext()))
        }
    }

    private fun handleStateChange(viewState: AnalyticsViewState) {
        binding.analyticsDateSelectorCard.updateSelectionTitle(viewState.analyticsDateRangeSelectorState.selectionType)
        binding.analyticsDateSelectorCard.updatePreviousRange(viewState.analyticsDateRangeSelectorState.previousRange)
        binding.analyticsDateSelectorCard.updateCurrentRange(viewState.analyticsDateRangeSelectorState.currentRange)
        binding.analyticsDateSelectorCard.updateLastUpdateTimestamp(viewState.lastUpdateTimestamp)
        binding.analyticsCallToActionCard.updateInformation(viewState.ctaState)
        when (viewState.cards) {
            is AnalyticsHubCardViewState.CardsState -> {
                (binding.cards.adapter as AnalyticsHubCardsAdapter).cardList = viewState.cards.cardsState
            }

            else -> {}
        }
        binding.analyticsRefreshLayout.isRefreshing = viewState.refreshIndicator == ShowIndicator
        displayFeedbackBanner(viewState.showFeedBackBanner)
    }

    private fun getDateRangeSelectorViewState() = viewModel.viewState.value.analyticsDateRangeSelectorState

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

    private fun setupMenu() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_analytics_settings, menu)
                }

                override fun onMenuItemSelected(item: MenuItem): Boolean {
                    if (item.itemId == R.id.menu_settings) {
                        viewModel.onOpenSettings()
                        return true
                    }

                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun startGoogleAdsWebView(
        url: String,
        title: String,
        isCreationFlow: Boolean
    ) {
        val direction = NavGraphMainDirections.actionGlobalGoogleAdsWebViewFragment(
            urlToLoad = url,
            title = title,
            urlComparisonMode = PARTIAL,
            isCreationFlow = isCreationFlow,
            entryPointSource = ANALYTICS_HUB
        )

        findNavController().navigateSafely(direction)
    }
}
