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
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.model.WCTopEarnerModel
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
        get() = childFragmentManager.backStackEntryCount == 0

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
                    presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity, forced = true)
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
            presenter.loadStats(dashboard_stats.activeGranularity)
            dashboard_top_earners.initView(listener = this, selectedSite = selectedSite)
            presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity)
        } else {
            loadDataPending = true
        }
    }

    override fun onBackStackChanged() {
        super.onBackStackChanged()
        // If this fragment is now visible and we've deferred loading data due to it not
        // being visible - go ahead and load the data.
        if (isActive && loadDataPending) {
            loadDataPending = false
            setLoadingIndicator(true)
            presenter.loadStats(dashboard_stats.activeGranularity)
            presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity)
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

    override fun showTopEarners(topEarnerList: List<WCTopEarnerModel>, granularity: StatsGranularity) {
        if (dashboard_top_earners.activeGranularity == granularity) {
            dashboard_top_earners.updateView(topEarnerList, granularity)
            dashboard_top_earners.visibility = View.VISIBLE
        }
    }

    override fun hideTopEarners() {
        dashboard_top_earners.visibility = View.GONE
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.dashboard)
    }

    override fun refreshFragmentState() {
        if (isActive) {
            setLoadingIndicator(true)
            presenter.loadStats(dashboard_stats.activeGranularity, forced = true)
            presenter.loadTopEarnerStats(dashboard_top_earners.activeGranularity, forced = true)
        } else {
            loadDataPending = true
        }
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        presenter.loadStats(period)
    }

    override fun onRequestLoadTopEarnerStats(period: StatsGranularity) {
        presenter.loadTopEarnerStats(period)
    }
}
