package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.TopLevelFragmentRouter
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View, DashboardStatsListener {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
    }

    @Inject lateinit var presenter: DashboardContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private var loadDataPending = false // If true, the fragment will refresh its data when it's visible

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
                    setLoadingIndicator(true)
                    presenter.loadStats(dashboard_stats.activeGranularity, forced = true)
                    presenter.loadOrdersToFulfillCount()
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        presenter.takeView(this)

        if (isActive) {
            setLoadingIndicator(true)
            dashboard_stats.initView(listener = this, selectedSite = selectedSite)
            dashboard_orders.initView(object : DashboardFulfillOrdersCard.Listener {
                override fun onViewOrdersClicked() {
                    if (activity is TopLevelFragmentRouter) {
                        (activity as TopLevelFragmentRouter).showOrderList(CoreOrderStatus.PROCESSING.value)
                    }
                }
            })
            presenter.loadStats(dashboard_stats.activeGranularity)
            presenter.loadOrdersToFulfillCount()
        } else {
            loadDataPending = true
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // If this fragment is now visible and we've deferred loading data due to it not
        // being visible - go ahead and load the data.
        if (isActive && loadDataPending) {
            loadDataPending = false
            setLoadingIndicator(true)
            presenter.loadStats(dashboard_stats.activeGranularity)
            presenter.loadOrdersToFulfillCount()
        }
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
            dashboard_stats.updateView(revenueStats, salesStats, presenter.getStatsCurrency())
            setLoadingIndicator(false)
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.dashboard)
    }

    override fun refreshFragmentState() {
        if (isActive) {
            setLoadingIndicator(true)
            presenter.loadStats(dashboard_stats.activeGranularity, forced = true)
            presenter.loadOrdersToFulfillCount()
        } else {
            loadDataPending = true
        }
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        presenter.loadStats(period)
    }

    override fun hideOrdersCard() {
        dashboard_orders.visibility = View.GONE
    }

    override fun showOrdersCard(count: Int) {
        dashboard_orders.updateOrdersCount(count)
        dashboard_orders.visibility = View.VISIBLE
    }
}
