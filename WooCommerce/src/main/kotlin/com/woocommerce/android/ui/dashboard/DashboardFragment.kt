package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.dashboard.DashboardStatsView.StatsTimeframe
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import javax.inject.Inject

class DashboardFragment : TopLevelFragment(), DashboardContract.View {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
    }

    @Inject lateinit var presenter: DashboardContract.Presenter

    override var isActive: Boolean = false
        get() = isAdded

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
                    presenter.loadStats(dashboard_stats.getActiveTimeframe())
                }
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO: Don't load stats if this fragment is not visible to the user, deferring it to onBackStackChanged()
        presenter.takeView(this)
        dashboard_stats.initView()
        presenter.loadStats(dashboard_stats.getActiveTimeframe())
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
        val update = when (dashboard_stats.getActiveTimeframe()) {
            StatsTimeframe.THIS_WEEK, StatsTimeframe.THIS_MONTH -> granularity == StatsGranularity.DAYS
            StatsTimeframe.THIS_YEAR -> granularity == StatsGranularity.MONTHS
            StatsTimeframe.YEARS -> granularity == StatsGranularity.YEARS
        }

        if (update) {
            dashboard_stats.populateView(revenueStats, salesStats, presenter.getStatsCurrency())
            setLoadingIndicator(false)
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.dashboard)
    }

    override fun refreshFragmentState() {
        setLoadingIndicator(true)
        presenter.loadStats(dashboard_stats.getActiveTimeframe())
    }
}
