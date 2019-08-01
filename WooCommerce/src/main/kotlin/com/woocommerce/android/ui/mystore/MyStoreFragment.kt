package com.woocommerce.android.ui.mystore

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
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dashboard.DashboardStatsListener
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.CurrencyFormatter
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_my_store.*
import kotlinx.android.synthetic.main.fragment_my_store.view.*
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class MyStoreFragment : TopLevelFragment(),
        MyStoreContract.View,
        DashboardStatsListener {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName
        const val STATE_KEY_TAB_STATS = "tab-stats-state"
        const val STATE_KEY_TAB_EARNERS = "tab-earners-state"
        const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
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

    override fun onAttach(context: Context?) {
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

                    MyStorePresenter.resetForceRefresh()
                    dashboard_refresh_layout.isRefreshing = false
                    refreshMyStoreStats(forced = true)
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        savedInstanceState?.let { bundle ->
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            my_store_stats.tabStateStats = bundle.getSerializable(STATE_KEY_TAB_STATS)
            my_store_top_earners.tabStateStats = bundle.getSerializable(STATE_KEY_TAB_EARNERS)
        }

        presenter.takeView(this)

        empty_view.setSiteToShare(selectedSite.get(), Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)

        my_store_stats.initView(
                my_store_stats.activeGranularity,
                listener = this,
                selectedSite = selectedSite,
                formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded)
        my_store_top_earners.initView(
                listener = this,
                selectedSite = selectedSite,
                formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded)

        scroll_view.setOnScrollChangeListener {
            v: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if (scrollY > oldScrollY) {
                onScrollDown()
            } else if (scrollY < oldScrollY) {
                onScrollUp()
            }
        }

        if (isActive && !deferInit) {
            isStatsRefreshed = true
            refreshMyStoreStats(forced = this.isRefreshPending)
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
            refreshMyStoreStats(forced = false)
        } else {
            isStatsRefreshed = false
        }
    }

    override fun onReturnedFromChildFragment() {
        // If this fragment is now visible and we've deferred loading stats due to it not
        // being visible - go ahead and load the stats.
        if (!deferInit) {
            refreshMyStoreStats(forced = this.isRefreshPending)
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
        outState.putSerializable(STATE_KEY_TAB_STATS, my_store_stats.activeGranularity)
        outState.putSerializable(STATE_KEY_TAB_EARNERS, my_store_stats.activeGranularity)
    }

    override fun showStats(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity
    ) {
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (my_store_stats.activeGranularity == granularity) {
            my_store_stats.showErrorView(false)
            my_store_stats.updateView(revenueStatsModel, presenter.getStatsCurrency())
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (my_store_stats.activeGranularity == granularity) {
            showStats(null, granularity)
            my_store_stats.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity) {
        if (my_store_stats.activeGranularity == granularity) {
            my_store_top_earners.showErrorView(false)
            my_store_top_earners.updateView(topEarnerList)
        }
    }

    override fun showTopEarnersError(granularity: StatsGranularity) {
        if (my_store_stats.activeGranularity == granularity) {
            my_store_top_earners.updateView(emptyList())
            my_store_top_earners.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showVisitorStats(visits: Int, granularity: StatsGranularity) {
        if (my_store_stats.activeGranularity == granularity) {
            my_store_stats.showVisitorStats(visits)
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (my_store_stats.activeGranularity == granularity) {
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
                }
                presenter.loadStats(my_store_stats.activeGranularity, forced)
                presenter.loadTopEarnerStats(my_store_stats.activeGranularity, forced)
                presenter.fetchHasOrders()
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
        my_store_top_earners.loadTopEarnerStats(period)
    }

    override fun onRequestLoadTopEarnerStats(period: StatsGranularity) {
        my_store_top_earners.showErrorView(false)
        presenter.loadTopEarnerStats(period)
    }

    override fun onTopEarnerClicked(topEarner: WCTopEarnerModel) {
        (activity as? MainNavigationRouter)?.showProductDetail(topEarner.id)
    }

    override fun showEmptyView(show: Boolean) {
        if (show) empty_view.show(R.string.waiting_for_customers) else empty_view.hide()
    }
}
