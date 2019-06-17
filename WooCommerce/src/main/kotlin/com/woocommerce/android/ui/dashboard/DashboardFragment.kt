package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View, DashboardStatsListener {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        const val STATE_KEY_TAB_STATS = "tab-stats-state"
        const val STATE_KEY_TAB_EARNERS = "tab-earners-state"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        fun newInstance() = DashboardFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    @Inject lateinit var presenter: DashboardContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateFragmentView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        with(view) {
            dashboard_refresh_layout.apply {
                activity?.let { activity ->
                    setColorSchemeColors(
                            ContextCompat.getColor(activity, R.color.colorPrimary),
                            ContextCompat.getColor(activity, R.color.colorAccent),
                            ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                    )
                }
                setOnRefreshListener {
                    // Track the user gesture
                    AnalyticsTracker.track(Stat.DASHBOARD_PULLED_TO_REFRESH)

                    DashboardPresenter.resetForceRefresh()
                    dashboard_refresh_layout.isRefreshing = false
                    refreshDashboard(forced = true)
                }
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
        }

        presenter.takeView(this)

        empty_view.setSiteToShare(selectedSite.get(), Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)

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

        dashboard_unfilled_orders.initView(object : DashboardUnfilledOrdersCard.Listener {
            override fun onViewOrdersClicked() {
                (activity as? TopLevelFragmentRouter)?.showOrderList(CoreOrderStatus.PROCESSING.value)
            }
        })

        scroll_view.setOnScrollChangeListener {
            v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if (scrollY > oldScrollY) {
                onScrollDown()
            } else if (scrollY < oldScrollY) {
                onScrollUp()
            }
        }

        if (isActive && !deferInit) {
            refreshDashboard(forced = this.isRefreshPending)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // silently refresh if this fragment is no longer hidden
        if (!isHidden) {
            refreshDashboard(forced = false)
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
    }

    override fun onReturnedFromChildFragment() {
        // If this fragment is now visible and we've deferred loading stats due to it not
        // being visible - go ahead and load the stats.
        if (!deferInit) {
            refreshDashboard(forced = this.isRefreshPending)
        }
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

    override fun showVisitorStats(visits: Int, granularity: StatsGranularity) {
        if (dashboard_stats.activeGranularity == granularity) {
            dashboard_stats.showVisitorStats(visits)
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (dashboard_stats.activeGranularity == granularity) {
            dashboard_stats.showVisitorStatsError()
        }
    }

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued() == true) {
            return
        }
        errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
        errorSnackbar?.show()
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
                presenter.fetchUnfilledOrderCount(forced)
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

    override fun showUnfilledOrdersSkeleton(show: Boolean) {
        dashboard_unfilled_orders.showSkeleton(show)
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
        (activity as? MainNavigationRouter)?.showProductDetail(topEarner.id)
    }

    override fun hideUnfilledOrdersCard() {
        if (dashboard_unfilled_orders.visibility == View.VISIBLE) {
            WooAnimUtils.scaleOut(dashboard_unfilled_orders, Duration.SHORT)
        }
    }

    override fun showUnfilledOrdersCard(count: Int) {
        dashboard_unfilled_orders.updateOrdersCount(count)
        if (dashboard_unfilled_orders.visibility != View.VISIBLE) {
            WooAnimUtils.scaleIn(dashboard_unfilled_orders, Duration.MEDIUM)
        }
    }

    override fun showEmptyView(show: Boolean) {
        if (show) empty_view.show(R.string.waiting_for_customers) else empty_view.hide()
    }
}
