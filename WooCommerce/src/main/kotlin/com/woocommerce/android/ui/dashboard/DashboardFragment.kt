package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.mystore.MyStoreStatsAvailabilityListener
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.dashboard_refresh_layout
import kotlinx.android.synthetic.main.fragment_dashboard.view.scroll_view
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View, DashboardStatsListener,
        MyStoreStatsAvailabilityListener {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        private const val STATE_KEY_TAB_STATS = "tab-stats-state"
        private const val STATE_KEY_TAB_EARNERS = "tab-earners-state"
        private const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        private const val STATE_KEY_IS_EMPTY_VIEW_SHOWING = "is-empty-view-showing"

        fun newInstance() = DashboardFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    @Inject lateinit var presenter: DashboardContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

    // If false, the fragment will refresh its data when it's visible on onHiddenChanged
    // this is to prevent the stats getting refreshed twice when the fragment is loaded when app is closed and opened
    private var isStatsRefreshed: Boolean = false

    private val mainActivity
        get() = activity as? MainActivity

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
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        with(view) {
            dashboard_refresh_layout.apply {
                setOnRefreshListener {
                    // Track the user gesture
                    AnalyticsTracker.track(Stat.DASHBOARD_PULLED_TO_REFRESH)

                    // check for new revenue stats availability
                    mainActivity?.fetchRevenueStatsAvailability(selectedSite.get())

                    DashboardPresenter.resetForceRefresh()
                    dashboard_refresh_layout.isRefreshing = false
                    refreshDashboard(forced = true)
                }
                scrollUpChild = scroll_view
            }

            if (FeatureFlag.APP_FEEDBACK.isEnabled()) {
                dashboard_feedback_request_card.visibility = View.VISIBLE
                val positiveCallback = { AppRatingDialog.showRateDialog(context) }
                val negativeCallback = { mainNavigationRouter?.showFeedbackSurvey() ?: Unit }
                dashboard_feedback_request_card.initView(negativeCallback, positiveCallback)
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedInstanceState?.let { bundle ->
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            dashboard_stats.tabStateStats = bundle.getSerializable(STATE_KEY_TAB_STATS)
            dashboard_top_earners.tabStateStats = bundle.getSerializable(STATE_KEY_TAB_EARNERS)
            if (bundle.getBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING)) {
                showEmptyView(true)
            }
        }

        presenter.takeView(this)

        dashboard_stats.initView(
                dashboard_stats.activeGranularity,
                listener = this,
                selectedSite = selectedSite,
                formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded)
        dashboard_top_earners.initView(
                dashboard_top_earners.activeGranularity,
                listener = this,
                selectedSite = selectedSite,
                formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded)

        if (isActive && !deferInit) {
            isStatsRefreshed = true
            refreshDashboard(forced = this.isRefreshPending)
        }

        if (AppPrefs.isV4StatsSupported()) {
            mainActivity?.replaceStatsFragment()
        } else if (AppPrefs.shouldDisplayV4StatsRevertedBanner()) {
            showV4StatsRevertedBanner(true)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // silently refresh if this fragment is no longer hidden
        if (!isHidden && !isStatsRefreshed) {
            refreshDashboard(forced = false)
        } else {
            isStatsRefreshed = false
        }
    }

    override fun onReturnedFromChildFragment() {
        // If this fragment is now visible and we've deferred loading stats due to it not
        // being visible - go ahead and load the stats.
        if (!deferInit) {
            refreshDashboard(forced = this.isRefreshPending)
        }
    }

    override fun onStop() {
        errorSnackbar?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putSerializable(STATE_KEY_TAB_STATS, dashboard_stats.activeGranularity)
        outState.putSerializable(STATE_KEY_TAB_EARNERS, dashboard_top_earners.activeGranularity)
        outState.putBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING, isEmptyViewShowing())
    }

    override fun showStats(
        revenueStats: Map<String, Double>,
        salesStats: Map<String, Int>,
        granularity: StatsGranularity
    ) {
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (dashboard_stats.activeGranularity == granularity) {
            dashboard_stats.showErrorView(false)
            dashboard_stats.updateView(revenueStats, salesStats, presenter.getStatsCurrency())
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (dashboard_stats.activeGranularity == granularity) {
            showStats(emptyMap(), emptyMap(), granularity)
            dashboard_stats.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity) {
        if (dashboard_top_earners.activeGranularity == granularity) {
            dashboard_top_earners.showErrorView(false)
            dashboard_top_earners.updateView(topEarnerList)
        }
    }

    override fun showTopEarnersError(granularity: StatsGranularity) {
        if (dashboard_top_earners.activeGranularity == granularity) {
            dashboard_top_earners.updateView(emptyList())
            dashboard_top_earners.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity) {
        if (dashboard_stats.activeGranularity == granularity) {
            dashboard_stats.showVisitorStats(visitorStats)
        }

        if (granularity == StatsGranularity.DAYS) {
            empty_stats_view.updateVisitorCount(visitorStats.values.sum())
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (dashboard_stats.activeGranularity == granularity) {
            dashboard_stats.showVisitorStatsError()
        }
    }

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == true) {
            return
        }
        errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
        errorSnackbar?.show()
    }

    override fun showV4StatsRevertedBanner(show: Boolean) {
        if (show) {
            dashboard_stats_reverted_card.visibility = View.VISIBLE
            dashboard_stats_reverted_card.initView(this)
        } else {
            dashboard_stats_reverted_card.visibility = View.GONE
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
        scroll_view.smoothScrollTo(0, 0)
    }

    override fun refreshFragmentState() {
        DashboardPresenter.resetForceRefresh()
        refreshDashboard(forced = false)
    }

    override fun refreshDashboard(forced: Boolean) {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        when {
            isActive -> {
                isRefreshPending = false
                if (forced) {
                    dashboard_stats.clearLabelValues()
                    dashboard_stats.clearChartData()
                }
                presenter.loadStats(dashboard_stats.activeGranularity, forced)
                presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity, forced)
                presenter.fetchHasOrders()
            }
            else -> isRefreshPending = true
        }
    }

    override fun showChartSkeleton(show: Boolean) {
        dashboard_stats.showSkeleton(show)
    }

    override fun showTopEarnersSkeleton(show: Boolean) {
        dashboard_top_earners.showSkeleton(show)
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        dashboard_stats.showErrorView(false)
        presenter.loadStats(period)
    }

    override fun onRequestLoadTopEarnerStats(period: StatsGranularity) {
        dashboard_top_earners.showErrorView(false)
        presenter.loadTopEarnerStats(period)
    }

    override fun onTopEarnerClicked(topEarner: WCTopEarnerModel) {
        mainNavigationRouter?.showProductDetail(topEarner.id)
    }

    override fun showEmptyView(show: Boolean) {
        if (show) {
            empty_view_container.show()
            empty_view.show(EmptyViewType.DASHBOARD) {
                AnalyticsTracker.track(Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(requireActivity(), selectedSite.get().url)
            }
            dashboard_view.hide()
        } else {
            empty_view_container.hide()
            dashboard_view.show()
        }
    }

    private fun isEmptyViewShowing() = empty_view_container.visibility == View.VISIBLE

    /**
     * Method called when the [com.woocommerce.android.ui.mystore.MyStoreStatsRevertedNoticeCard] banner is dismissed.
     * The banner will no longer be displayed to the user
     */
    override fun onMyStoreStatsRevertedNoticeCardDismissed() {
        AppPrefs.setShouldDisplayV4StatsRevertedBanner(false)
        showV4StatsRevertedBanner(false)
    }
}
