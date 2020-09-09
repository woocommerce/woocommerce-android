package com.woocommerce.android.ui.mystore

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.containsInstanceOf
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_my_store.*
import kotlinx.android.synthetic.main.fragment_my_store.view.*
import kotlinx.android.synthetic.main.my_store_stats.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class MyStoreFragment : TopLevelFragment(),
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

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

    // If false, the fragment will refresh its data when it's visible on onHiddenChanged
    // this is to prevent the stats getting refreshed twice when the fragment is loaded when app is closed and opened
    private var isStatsRefreshed: Boolean = false

    private var tabStatsPosition: Int = 0 // Save the current position of stats tab view
    private val activeGranularity: StatsGranularity
        get() {
            return tabLayout.getTabAt(tabStatsPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private val tabLayout: TabLayout by lazy {
        TabLayout(requireContext(), null, R.attr.scrollableTabStyle).also {
            it.setId(R.id.stats_tab_layout)
        }
    }

    private val appBarLayout
        get() = activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_store, container, false)
        with(view) {
            dashboard_refresh_layout.apply {
                setOnRefreshListener {
                    // Track the user gesture
                    AnalyticsTracker.track(Stat.DASHBOARD_PULLED_TO_REFRESH)

                    MyStorePresenter.resetForceRefresh()
                    dashboard_refresh_layout.isRefreshing = false
                    refreshMyStoreStats(forced = true)
                }
            }

            if (FeatureFlag.APP_FEEDBACK.isEnabled()) {
                store_feedback_request_card.visibility = View.VISIBLE
                val positiveCallback = { AppRatingDialog.showRateDialog(context) }
                val negativeCallback = {
                    mainNavigationRouter?.showFeedbackSurvey()
                    removeTabLayoutFromAppBar(tabLayout)
                }
                store_feedback_request_card.initView(negativeCallback, positiveCallback)
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

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
                setText(my_store_stats.getStringForGranularity(granularity))
                tag = granularity
            }
            tabLayout.addTab(tab)

            // Start with the given time period selected
            if (granularity == activeGranularity) {
                tab.select()
            }
        }

        my_store_date_bar.initView()
        my_store_stats.initView(
            activeGranularity,
            listener = this,
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded
        )
        my_store_top_earners.initView(
            listener = this,
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded,
            statsCurrencyCode = presenter.getStatsCurrency().orEmpty()
        )

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabStatsPosition = tab.position
                my_store_date_bar?.clearDateRangeValues()
                my_store_stats?.loadDashboardStats(activeGranularity)
                my_store_top_earners?.loadTopEarnerStats(activeGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        if (isActive && !deferInit) {
            isStatsRefreshed = true
            refreshMyStoreStats(forced = this.isRefreshPending)
        }
    }

    override fun onResume() {
        super.onResume()
        addTabLayoutToAppBar(tabLayout)
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!isHidden) {
            if (!isStatsRefreshed) {
                // silently refresh if this fragment is no longer hidden
                refreshMyStoreStats(forced = false)
            }
            addTabLayoutToAppBar(tabLayout)
        } else {
            isStatsRefreshed = false
            removeTabLayoutFromAppBar(tabLayout)
        }
    }

    override fun onReturnedFromChildFragment() {
        // If this fragment is now visible and we've deferred loading stats due to it not
        // being visible - go ahead and load the stats.
        if (!deferInit) {
            refreshMyStoreStats(forced = this.isRefreshPending)
        }
        addTabLayoutToAppBar(tabLayout)
    }

    override fun onStop() {
        errorSnackbar?.dismiss()
        removeTabLayoutFromAppBar(tabLayout)
        super.onStop()
    }

    override fun onDestroyView() {
        my_store_stats.removeListener()
        my_store_top_earners.removeListener()
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putInt(STATE_KEY_TAB_POSITION, tabLayout.selectedTabPosition)
        outState.putBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING, isEmptyViewShowing())
    }

    override fun showStats(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity
    ) {
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (activeGranularity == granularity) {
            my_store_stats.showErrorView(false)
            my_store_stats.updateView(revenueStatsModel, presenter.getStatsCurrency())
            my_store_date_bar.updateDateRangeView(revenueStatsModel, granularity)
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            showStats(null, granularity)
            my_store_stats.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showTopPerformers(topPerformers: List<WCTopPerformerProductModel>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            my_store_top_earners.showErrorView(false)
            my_store_top_earners.updateView(topPerformers)
        }
    }

    override fun showTopPerformersError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            my_store_top_earners.updateView(emptyList())
            my_store_top_earners.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            my_store_stats.showVisitorStats(visitorStats)
            if (granularity == StatsGranularity.DAYS) {
                empty_stats_view.updateVisitorCount(visitorStats.values.sum())
            }
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            my_store_stats.showVisitorStatsError()
        }
    }

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == true) {
            return
        }
        errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
        errorSnackbar?.show()
    }

    override fun updateStatsAvailabilityError() {
        (activity as? MainActivity)?.updateStatsView(false)
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
        scroll_view.smoothScrollTo(0, 0)
    }

    override fun refreshFragmentState() {
        MyStorePresenter.resetForceRefresh()
        refreshMyStoreStats(forced = false)
    }

    override fun refreshMyStoreStats(forced: Boolean) {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        when {
            isActive -> {
                isRefreshPending = false
                if (forced) {
                    my_store_stats.clearLabelValues()
                    my_store_stats.clearChartData()
                    my_store_date_bar.clearDateRangeValues()
                }
                presenter.run {
                    loadStats(activeGranularity, forced)
                    coroutineScope.launch { loadTopPerformersStats(activeGranularity, forced) }
                    fetchHasOrders()
                }
            }
            else -> isRefreshPending = true
        }
    }

    override fun showChartSkeleton(show: Boolean) {
        my_store_stats.showSkeleton(show)
    }

    override fun showTopEarnersSkeleton(show: Boolean) {
        my_store_top_earners.showSkeleton(show)
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        my_store_stats.showErrorView(false)
        presenter.loadStats(period)
    }

    override fun onRequestLoadTopEarnerStats(period: StatsGranularity) {
        my_store_top_earners.showErrorView(false)
        presenter.coroutineScope.launch {
            presenter.loadTopPerformersStats(period)
        }
    }

    override fun onTopPerformerClicked(topPerformer: WCTopPerformerProductModel) {
        removeTabLayoutFromAppBar(tabLayout)
        mainNavigationRouter?.showProductDetail(topPerformer.product.remoteProductId)
    }

    override fun onChartValueSelected(dateString: String, period: StatsGranularity) {
        my_store_date_bar.updateDateViewOnScrubbing(dateString, period)
    }

    override fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {
        my_store_date_bar.updateDateRangeView(revenueStatsModel, period)
    }

    override fun showEmptyView(show: Boolean) {
        val dashboardVisibility: Int
        if (show) {
            dashboardVisibility = View.GONE
            empty_view.show(EmptyViewType.DASHBOARD) {
                AnalyticsTracker.track(Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(requireActivity(), selectedSite.get().url)
            }
            empty_stats_view.visibility = View.VISIBLE
        } else {
            empty_view.hide()
            dashboardVisibility = View.VISIBLE
            empty_stats_view.visibility = View.GONE
        }

        tabLayout.visibility = dashboardVisibility
        my_store_date_bar.visibility = dashboardVisibility
        my_store_stats.visibility = dashboardVisibility
        my_store_top_earners.visibility = dashboardVisibility
    }

    private fun addTabLayoutToAppBar(tabLayout: TabLayout) {
        appBarLayout
            ?.takeIf { isActive && !it.children.containsInstanceOf(tabLayout) }
            ?.addView(
                tabLayout,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            )
    }

    private fun removeTabLayoutFromAppBar(tabLayout: TabLayout) {
        appBarLayout?.removeView(tabLayout)
    }

    private fun isEmptyViewShowing() = empty_view.visibility == View.VISIBLE
}
