package com.woocommerce.android.ui.mystore

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.FeedbackPrefs.userFeedbackIsDue
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentMyStoreBinding
import com.woocommerce.android.extensions.configureStringClick
import com.woocommerce.android.extensions.containsInstanceOf
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject

class MyStoreFragment : TopLevelFragment(R.layout.fragment_my_store),
    MyStoreContract.View,
    MyStoreStatsListener {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName
        private const val STATE_KEY_TAB_POSITION = "tab-stats-position"
        private const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        private const val STATE_KEY_IS_EMPTY_VIEW_SHOWING = "is-empty-view-showing"

        fun newInstance() = MyStoreFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    @Inject lateinit var presenter: MyStoreContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var _binding: FragmentMyStoreBinding? = null
    private val binding get() = _binding!!

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

    private var tabStatsPosition: Int = 0 // Save the current position of stats tab view
    private val activeGranularity: StatsGranularity
        get() {
            return tabLayout.getTabAt(tabStatsPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private var _tabLayout: TabLayout? = null
    private val tabLayout
        get() = _tabLayout!!

    private val appBarLayout
        get() = activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    private val myStoreDateBar
        get() = binding.myStoreStats.myStoreDateBar

    private var isEmptyViewVisible: Boolean = false

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            tabStatsPosition = tab.position
            myStoreDateBar.clearDateRangeValues()
            binding.myStoreStats.loadDashboardStats(activeGranularity)
            binding.myStoreTopPerformers.loadTopPerformerStats(activeGranularity)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}

        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        _tabLayout = TabLayout(requireContext(), null, R.attr.scrollableTabStyle)
        addTabLayoutToAppBar()

        _binding = FragmentMyStoreBinding.bind(view)

        binding.myStoreRefreshLayout.setOnRefreshListener {
            // Track the user gesture
            AnalyticsTracker.track(Stat.DASHBOARD_PULLED_TO_REFRESH)

            MyStorePresenter.resetForceRefresh()
            binding.myStoreRefreshLayout.isRefreshing = false
            refreshMyStoreStats(forced = true)
        }

        savedInstanceState?.let { bundle ->
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            tabStatsPosition = bundle.getInt(STATE_KEY_TAB_POSITION)
            if (bundle.getBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING)) {
                showEmptyView(true)
            }
        }

        presenter.takeView(this)

        // Create tabs and add to appbar
        StatsGranularity.values().forEach { granularity ->
            val tab = tabLayout.newTab().apply {
                setText(binding.myStoreStats.getStringForGranularity(granularity))
                tag = granularity
            }
            tabLayout.addTab(tab)

            // Start with the given time period selected
            if (granularity == activeGranularity) {
                tab.select()
            }
        }

        myStoreDateBar.initView()

        binding.myStoreStats.initView(
            activeGranularity,
            listener = this,
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded
        )

        binding.myStoreTopPerformers.initView(
            listener = this,
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded,
            statsCurrencyCode = presenter.getStatsCurrency().orEmpty()
        )

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        getString(R.string.my_store_stats_availability_description, contactUsText)
            .configureStringClick(
                clickableContent = contactUsText,
                clickAction = WooClickableSpan { activity?.startHelpActivity(Origin.MY_STORE) },
                textField = binding.myStoreStatsAvailabilityMessage
            )

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        refreshMyStoreStats(forced = this.isRefreshPending)
    }

    override fun onResume() {
        super.onResume()
        handleFeedbackRequestCardState()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        errorSnackbar?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.myStoreStats.removeListener()
        binding.myStoreTopPerformers.removeListener()
        removeTabLayoutFromAppBar()
        tabLayout.removeOnTabSelectedListener(tabSelectedListener)
        _tabLayout = null
        presenter.dropView()
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_action_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                (activity as? MainNavigationRouter)?.showSettingsScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putInt(STATE_KEY_TAB_POSITION, tabStatsPosition)
        outState.putBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING, isEmptyViewVisible)
    }

    override fun showStats(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity
    ) {
        addTabLayoutToAppBar()
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (activeGranularity == granularity) {
            binding.myStoreStats.showErrorView(false)
            binding.myStoreStats.updateView(revenueStatsModel, presenter.getStatsCurrency())
            myStoreDateBar.updateDateRangeView(revenueStatsModel, granularity)
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            showStats(null, granularity)
            binding.myStoreStats.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun updateStatsAvailabilityError() {
        binding.myStoreRefreshLayout.visibility = View.GONE
        WooAnimUtils.fadeIn(binding.statsErrorScrollView)
        removeTabLayoutFromAppBar()
    }

    override fun showTopPerformers(topPerformers: List<WCTopPerformerProductModel>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreTopPerformers.showErrorView(false)
            binding.myStoreTopPerformers.updateView(topPerformers)
        }
    }

    override fun showTopPerformersError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreTopPerformers.updateView(emptyList())
            binding.myStoreTopPerformers.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreStats.showVisitorStats(visitorStats)
            if (granularity == StatsGranularity.DAYS) {
                binding.emptyStatsView.updateVisitorCount(visitorStats.values.sum())
            }
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreStats.showVisitorStatsError()
        }
    }

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == false || NetworkUtils.isNetworkAvailable(context)) {
            errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
            errorSnackbar?.show()
        }
    }

    override fun getFragmentTitle(): String {
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                return site.displayName
            } else if (!site.name.isNullOrBlank()) {
                return site.name
            }
        }
        return getString(R.string.my_store)
    }

    override fun scrollToTop() {
        binding.statsScrollView.smoothScrollTo(0, 0)
    }

    override fun refreshMyStoreStats(forced: Boolean) {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        if (forced) {
            binding.myStoreStats.clearLabelValues()
            binding.myStoreStats.clearChartData()
            myStoreDateBar.clearDateRangeValues()
        }
        presenter.run {
            loadStats(activeGranularity, forced)
            coroutineScope.launch { loadTopPerformersStats(activeGranularity, forced) }
            fetchHasOrders()
        }
    }

    override fun showChartSkeleton(show: Boolean) {
        binding.myStoreStats.showSkeleton(show)
    }

    override fun showTopPerformersSkeleton(show: Boolean) {
        binding.myStoreTopPerformers.showSkeleton(show)
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        binding.myStoreStats.showErrorView(false)
        presenter.loadStats(period)
    }

    override fun onRequestLoadTopPerformersStats(period: StatsGranularity) {
        binding.myStoreTopPerformers.showErrorView(false)
        presenter.coroutineScope.launch {
            presenter.loadTopPerformersStats(period)
        }
    }

    override fun onTopPerformerClicked(topPerformer: WCTopPerformerProductModel) {
        mainNavigationRouter?.showProductDetail(topPerformer.product.remoteProductId)
    }

    override fun onChartValueSelected(dateString: String, period: StatsGranularity) {
        myStoreDateBar.updateDateViewOnScrubbing(dateString, period)
    }

    override fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {
        myStoreDateBar.updateDateRangeView(revenueStatsModel, period)
    }

    /**
     * This method verifies if the feedback card should be visible.
     *
     * If it should but it's not, the feedback card is reconfigured and presented
     * If should not and it's visible, the card visibility is changed to gone
     * If should be and it's already visible, nothing happens
     */
    private fun handleFeedbackRequestCardState() = with(binding.storeFeedbackRequestCard) {
        if (userFeedbackIsDue && visibility == View.GONE) {
            setupFeedbackRequestCard()
        } else if (userFeedbackIsDue.not() && visibility == View.VISIBLE) {
            visibility = View.GONE
        }
    }

    private fun setupFeedbackRequestCard() {
        binding.storeFeedbackRequestCard.visibility = View.VISIBLE
        val negativeCallback = {
            mainNavigationRouter?.showFeedbackSurvey()
            binding.storeFeedbackRequestCard.visibility = View.GONE
            FeedbackPrefs.lastFeedbackDate = Calendar.getInstance().time
        }
        binding.storeFeedbackRequestCard.initView(negativeCallback, ::handleFeedbackRequestPositiveClick)
    }

    private fun handleFeedbackRequestPositiveClick() {
        context?.let {
            // Hide the card and set last feedback date to now
            binding.storeFeedbackRequestCard.visibility = View.GONE
            FeedbackPrefs.lastFeedbackDate = Calendar.getInstance().time

            // Request a ReviewInfo object from the Google Reviews API. If this fails
            // we just move on as there isn't anything we can do.
            val manager = ReviewManagerFactory.create(requireContext())
            val reviewRequest = manager.requestReviewFlow()
            reviewRequest.addOnCompleteListener {
                if (it.isSuccessful) {
                    // Request to start the Review flow so the user can be prompted to submit
                    // a play store review. The prompt will only appear if the user hasn't already
                    // reached their quota for how often we can ask for a review.
                    val reviewInfo = it.result
                    val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                    flow.addOnFailureListener { ex ->
                        WooLog.e(WooLog.T.DASHBOARD, "Error launching google review API flow.", ex)
                    }
                } else {
                    // There was an error, just log and continue. Google doesn't really tell you what
                    // type of scenario would cause an error.
                    WooLog.e(
                        WooLog.T.DASHBOARD,
                        "Error fetching ReviewInfo object from Review API to start in-app review process",
                        it.exception
                    )
                }
            }
        }
    }

    override fun showEmptyView(show: Boolean) {
        val dashboardVisibility: Int
        if (show) {
            dashboardVisibility = View.GONE
            binding.emptyView.show(EmptyViewType.DASHBOARD) {
                AnalyticsTracker.track(Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(requireActivity(), selectedSite.get().url)
            }
            binding.emptyStatsView.visibility = View.VISIBLE
        } else {
            binding.emptyView.hide()
            dashboardVisibility = View.VISIBLE
            binding.emptyStatsView.visibility = View.GONE
        }

        tabLayout.visibility = dashboardVisibility
        myStoreDateBar.visibility = dashboardVisibility
        binding.myStoreStats.visibility = dashboardVisibility
        binding.myStoreTopPerformers.visibility = dashboardVisibility
        isEmptyViewVisible = show
    }

    private fun addTabLayoutToAppBar() {
        appBarLayout
            ?.takeIf { !it.children.containsInstanceOf(tabLayout) }
            ?.takeIf { AppPrefs.isV4StatsSupported() }
            ?.addView(
                tabLayout,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
    }

    private fun removeTabLayoutFromAppBar() {
        appBarLayout?.removeView(tabLayout)
    }

    override fun isScrolledToTop() = binding.statsScrollView.scrollY == 0
}
