package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View, DashboardStatsListener {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
    }

    @Inject lateinit var presenter: DashboardContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
    private var errorSnackbar: Snackbar? = null

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
                    presenter.resetTopEarnersForceRefresh()
                    refreshDashboard()
                }
            }

            no_orders_image.visibility =
                    if (DisplayUtils.isLandscape(activity)) View.GONE else View.VISIBLE
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        if (isActive) {
            dashboard_stats.initView(listener = this, selectedSite = selectedSite)
            dashboard_unfilled_orders.initView(object : DashboardUnfilledOrdersCard.Listener {
                override fun onViewOrdersClicked() {
                    (activity as? TopLevelFragmentRouter)?.showOrderList(CoreOrderStatus.PROCESSING.value)
                }
            })

            dashboard_top_earners.initView(listener = this, selectedSite = selectedSite)
            refreshDashboard()
        } else {
            isRefreshPending = true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // If this fragment is now visible and we've deferred loading data due to it not
        // being visible - go ahead and load the data.
        if (isActive && isRefreshPending) {
            refreshDashboard()
        }
    }

    /**
     * the main activity has `android:configChanges="orientation|screenSize"` in the manifest, so we have to
     * handle screen rotation here
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        newConfig?.let {
            no_orders_image.visibility =
                    if (it.orientation == ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
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

    override fun setLoadingIndicator(active: Boolean) {
        with(dashboard_refresh_layout) {
            // Make sure this is called after the layout is done with everything else.
            post { isRefreshing = active }
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
            setLoadingIndicator(false)
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

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued() == true) {
            return
        }
        errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
        errorSnackbar?.show()
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.my_store)
    }

    override fun refreshFragmentState() {
        presenter.resetTopEarnersForceRefresh()
        refreshDashboard()
    }

    override fun refreshDashboard() {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        when {
            isActive -> {
                isRefreshPending = false
                presenter.loadStats(dashboard_stats.activeGranularity, forced = true)
                presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity, forced = true)
                presenter.fetchUnfilledOrderCount()
                presenter.fetchHasOrders()
            }
            else -> isRefreshPending = true
        }
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        dashboard_stats.showErrorView(false)
        presenter.loadStats(period)
    }

    override fun onRequestLoadTopEarnerStats(period: StatsGranularity) {
        dashboard_top_earners.showErrorView(false)
        presenter.loadTopEarnerStats(period)
    }

    override fun hideUnfilledOrdersCard() {
        if (dashboard_unfilled_orders.visibility == View.VISIBLE) {
            WooAnimUtils.scaleOut(dashboard_unfilled_orders, Duration.SHORT)
        }
    }

    override fun showUnfilledOrdersCard(count: Int, canLoadMore: Boolean) {
        dashboard_unfilled_orders.updateOrdersCount(count, canLoadMore)
        if (dashboard_unfilled_orders.visibility != View.VISIBLE) {
            WooAnimUtils.scaleIn(dashboard_unfilled_orders, Duration.MEDIUM)
        }
    }

    override fun showUnfilledOrdersProgress(show: Boolean) {
        dashboard_unfilled_orders.showProgress(show)
    }

    override fun shareStoreUrl() {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, selectedSite.get().url)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent,
                resources.getText(R.string.dashboard_no_orders_share_store_title)))
    }

    /**
     * shows the "waiting for customers" view that appears for stores that have never had any orders
     */
    override fun showNoOrdersView(show: Boolean) {
        if (show && no_orders_view.visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(no_orders_view, Duration.LONG)
            no_orders_share_button.setOnClickListener {
                shareStoreUrl()
            }
        } else if (!show && no_orders_view.visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(no_orders_view, Duration.LONG)
        }
    }
}
