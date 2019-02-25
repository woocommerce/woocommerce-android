package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.widget.NestedScrollView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dashboard.DashboardCustomStatsDialog.CustomStatsFieldListener
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.model.WCOrderStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View, DashboardStatsListener,
        CustomStatsFieldListener {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS

        private const val URL_UPGRADE_WOOCOMMERCE = "https://docs.woocommerce.com/document/how-to-update-woocommerce/"
    }

    @Inject lateinit var presenter: DashboardContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

    private var shouldCustomStatsDialogDisplay: Boolean = false
    private var customStatsDialog: DashboardCustomStatsDialog? = null

    override var isActive: Boolean = false
        get() = childFragmentManager.backStackEntryCount == 0 && !isHidden

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

                    presenter.resetForceRefresh()
                    dashboard_refresh_layout.isRefreshing = false
                    refreshDashboard(forced = true)
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        empty_view.setSiteToShare(selectedSite.get(), Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)

        dashboard_stats.initView(
                listener = this,
                selectedSite = selectedSite,
                formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded,
                customOrderStatsModel = presenter.getCustomOrderStats())
        dashboard_top_earners.initView(
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
            if (scrollY > oldScrollY) onScrollDown() else if (scrollY < oldScrollY) onScrollUp()
        }

        dashboard_plugin_version_notice.initView(
                title = getString(R.string.dashboard_plugin_notice_title),
                message = getString(R.string.dashboard_plugin_notice_message),
                buttonLabel = getString(R.string.button_update_instructions),
                buttonAction = { ChromeCustomTabUtils.launchUrl(activity as Context, URL_UPGRADE_WOOCOMMERCE) })

        if (isActive) {
            refreshDashboard(forced = this.isRefreshPending)
        } else {
            isRefreshPending = true
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // If this fragment is now visible and we've deferred loading data due to it not
        // being visible - go ahead and load the data.
        if (isActive && isRefreshPending) {
            refreshDashboard(forced = false)
        }
    }

    override fun onPause() {
        super.onPause()
        if (!shouldCustomStatsDialogDisplay) {
            customStatsDialog?.dismiss()
            customStatsDialog = null
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

    override fun showStats(
        revenueStats: Map<String, Double>,
        salesStats: Map<String, Int>,
        granularity: StatsGranularity
    ) {
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (dashboard_stats.isActiveTab(granularity)) {
            dashboard_stats.showErrorView(false)
            dashboard_stats.updateView(
                    revenueStats,
                    salesStats,
                    presenter.getStatsCurrency(),
                    presenter.getCustomOrderStats()
            )
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (dashboard_stats.isActiveTab(granularity)) {
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
        if (dashboard_stats.isActiveTab(granularity)) {
            dashboard_stats.showVisitorStats(visits)
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (dashboard_stats.isActiveTab(granularity)) {
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
        presenter.resetForceRefresh()
        refreshDashboard(forced = false)
    }

    override fun refreshDashboard(forced: Boolean) {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        when {
            isActive -> {
                isRefreshPending = false
                dashboard_stats.clearLabelValues()
                /**
                 * Refresh only the currently selected tab.
                 *
                 * If custom tab: need to pass startDate, endDate, granularity, forced
                 * If default tab: pass only granularity and forced
                 * */
                if (dashboard_stats.isCustomTab()) {
                    onRequestLoadCustomStats(dashboard_stats.wcOrderStatsModel)
                } else {
                    presenter.loadStats(dashboard_stats.activeGranularity, forced)
                }
                presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity, forced)
                presenter.fetchUnfilledOrderCount(forced)
                presenter.fetchHasOrders()
                if (!AppPrefs.isUsingV3Api()) {
                    presenter.checkApiVersion()
                }
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

    override fun onRequestLoadCustomStats(wcOrderStatsModel: WCOrderStatsModel?) {
        /**
         * The custom stats dialog should only be retained in onPause
         * only there is no custom stats available in cache
         * */
        shouldCustomStatsDialogDisplay = (wcOrderStatsModel == null)
        wcOrderStatsModel?.let {
            dashboard_stats.showErrorView(false)
            presenter.loadStats(
                    granularity = StatsGranularity.fromString(wcOrderStatsModel.unit),
                    forced = false,
                    startDate = wcOrderStatsModel.startDate,
                    endDate = wcOrderStatsModel.endDate)
        } ?: onRequestDateRangeSelector(wcOrderStatsModel)
    }

    override fun onRequestDateRangeSelector(wcOrderStatsModel: WCOrderStatsModel?) {
        customStatsDialog = DashboardCustomStatsDialog
                .newInstance(wcOrderStatsModel, listener = this)
                .also { it.show(fragmentManager, DashboardCustomStatsDialog.TAG) }
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

    override fun showPluginVersionNoticeCard() {
        if (dashboard_plugin_version_notice.visibility != View.VISIBLE) {
            WooAnimUtils.scaleIn(dashboard_plugin_version_notice, Duration.MEDIUM)
        }
    }

    override fun hidePluginVersionNoticeCard() {
        dashboard_plugin_version_notice.visibility = View.GONE
    }

    override fun showEmptyView(show: Boolean) {
        if (show) empty_view.show(R.string.waiting_for_customers) else empty_view.hide()
    }

    override fun onFieldSelected(startDate: String, endDate: String, granularity: StatsGranularity) {
        dashboard_stats.showErrorView(false)

        /**
         * If the incoming startDate, endDate & granularity
         * is different to the stored startDate, endDate & granularity,
         * then the assumption is that the data is being fetched from api
         * so boolean flag is set to true
         */
        val wcOrderStatsModel: WCOrderStatsModel? = presenter.getCustomOrderStats()
        val forced = wcOrderStatsModel?.let {
            !(startDate.equals(it.startDate) &&
                    endDate.equals(it.endDate) &&
                    granularity.equals(StatsGranularity.fromString(it.unit)))
        } ?: false

        dashboard_stats.updateDateRangeView(startDate, endDate, granularity)
        presenter.loadStats(
                granularity = granularity,
                forced = forced,
                startDate = startDate,
                endDate = endDate)
    }
}
