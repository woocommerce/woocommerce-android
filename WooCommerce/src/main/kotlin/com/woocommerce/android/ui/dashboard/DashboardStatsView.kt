package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardUtils.formatAmountForDisplay
import com.woocommerce.android.util.DateUtils
import kotlinx.android.synthetic.main.dashboard_stats.view.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.SiteUtils
import java.util.Timer
import java.util.TimerTask

class DashboardStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    companion object {
        private val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
        private const val PROGRESS_DELAY_TIME_MS = 200L
    }

    var activeGranularity: StatsGranularity = DEFAULT_STATS_GRANULARITY
        get() {
            return tab_layout.getTabAt(tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private lateinit var selectedSite: SelectedSite

    private var chartRevenueStats = mapOf<String, Double>()
    private var chartCurrencyCode: String? = null

    private var progressDelayTimer: Timer? = null
    private var progressDelayTimerTask: TimerTask? = null

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: DashboardStatsListener,
        selectedSite: SelectedSite
    ) {
        barchart_progress.visibility = View.VISIBLE

        this.selectedSite = selectedSite

        StatsGranularity.values().forEach { granularity ->
            val tab = tab_layout.newTab().apply {
                setText(DashboardUtils.getStringForGranularity(granularity))
                tag = granularity
            }
            tab_layout.addTab(tab)

            // Start with the given time period selected
            if (granularity == period) {
                tab.select()
            }
        }

        tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                revenue_value.text = ""
                orders_value.text = ""
                // Show the progress view after some delay
                // This gives us a chance to never show it at all when the stats data is cached and returns quickly,
                // preventing glitchy behavior
                showProgressDelayed()
                listener.onRequestLoadStats(tab.tag as StatsGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        initChart()
    }

    /**
     * One-time chart initialization with settings common to all granularities.
     */
    private fun initChart() {
        with (chart) {
            with (xAxis) {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time

                setLabelCount(2, true) // Only show first and last date

                valueFormatter = StartEndDateAxisFormatter()
            }

            with (axisLeft) {
                setDrawAxisLine(false)

                setDrawGridLines(true)
                enableGridDashedLine(10F, 10F, 0F)
                gridColor = ContextCompat.getColor(context, R.color.graph_grid_color)

                setDrawZeroLine(true)
                zeroLineWidth = 1F
                zeroLineColor = ContextCompat.getColor(context, R.color.graph_grid_color)

                axisMinimum = 0F

                valueFormatter = IAxisValueFormatter { value, _ ->
                    formatAmountForDisplay(context, value.toDouble(), chartCurrencyCode, allowZero = false)
                }
            }

            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false

            // This disables pinch to zoom and swiping through the zoomed graph
            // We can reenable it, but we'll probably want to disable pull-to-refresh inside the graph view
            setTouchEnabled(false)
        }
    }

    fun updateView(revenueStats: Map<String, Double>, orderStats: Map<String, Int>, currencyCode: String?) {
        progressDelayTimer?.cancel()
        progressDelayTimerTask?.cancel()
        barchart_progress.visibility = View.GONE

        chartCurrencyCode = currencyCode

        revenue_value.text = formatAmountForDisplay(context, revenueStats.values.sum(), currencyCode)
        orders_value.text = orderStats.values.sum().toString()

        if (revenueStats.isEmpty()) {
            // TODO Replace with custom empty view
            chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.graph_no_data_text_color))
            chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            chart.clear()
            return
        }

        val dataSet = generateBarDataSet(revenueStats).apply {
            color = ContextCompat.getColor(context, R.color.graph_data_color)
            setDrawValues(false)
            isHighlightEnabled = false
        }

        with (chart) {
            data = BarData(dataSet)

            invalidate() // Draw/redraw the graph
        }
    }

    private fun generateBarDataSet(revenueStats: Map<String, Double>): BarDataSet {
        val barEntries = when (activeGranularity) {
            StatsGranularity.DAYS,
            StatsGranularity.WEEKS,
            StatsGranularity.MONTHS -> {
                chartRevenueStats = revenueStats
                chartRevenueStats.values.mapIndexed { index, value ->
                    BarEntry((index + 1).toFloat(), value.toFloat())
                }
            }
            StatsGranularity.YEARS -> {
                // Clean up leading empty years and start from first year with non-zero sales
                // (but always include current year)
                val modifiedRevenueStats = revenueStats.toMutableMap()
                for (entry in revenueStats) {
                    if (entry.value != 0.0 || entry.key == revenueStats.keys.last()) {
                        break
                    }
                    modifiedRevenueStats.remove(entry.key)
                }
                chartRevenueStats = modifiedRevenueStats
                chartRevenueStats.map { BarEntry(it.key.toFloat(), it.value.toFloat()) }
            }
        }

        return BarDataSet(barEntries, "")
    }

    private fun showProgressDelayed() {
        progressDelayTimerTask = object : TimerTask() {
            override fun run() {
                this@DashboardStatsView.post {
                    barchart_progress.visibility = View.VISIBLE
                }
            }
        }

        progressDelayTimer = Timer().apply {
            schedule(progressDelayTimerTask, PROGRESS_DELAY_TIME_MS)
        }
    }

    private inner class StartEndDateAxisFormatter : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return when (value) {
                axis.mEntries.first() -> getStartValue()
                axis.mEntries.max() -> getEndValue()
                else -> ""
            }
        }

        fun getStartValue(): String {
            val dateString = chartRevenueStats.keys.first()
            return when (activeGranularity) {
                StatsGranularity.DAYS -> DateUtils.getShortMonthDayString(dateString)
                StatsGranularity.WEEKS -> DateUtils.getShortMonthDayStringForWeek(dateString)
                StatsGranularity.MONTHS -> DateUtils.getShortMonthString(dateString)
                StatsGranularity.YEARS -> dateString
            }
        }

        fun getEndValue(): String {
            val dateString = chartRevenueStats.keys.last()
            return when (activeGranularity) {
                StatsGranularity.DAYS -> DateUtils.getShortMonthDayString(dateString)
                StatsGranularity.WEEKS ->
                    SiteUtils.getCurrentDateTimeForSite(selectedSite.get(), DateUtils.friendlyMonthDayFormat)
                StatsGranularity.MONTHS -> DateUtils.getShortMonthString(dateString)
                StatsGranularity.YEARS -> dateString
            }
        }
    }
}
