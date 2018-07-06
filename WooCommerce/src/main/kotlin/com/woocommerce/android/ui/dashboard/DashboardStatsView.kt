package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyUtils
import com.woocommerce.android.util.DateUtils
import kotlinx.android.synthetic.main.dashboard_stats.view.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

class DashboardStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    fun initView(period: StatsGranularity = StatsGranularity.DAYS, listener: DashboardStatsListener) {
        barchart_progress.visibility = View.VISIBLE

        StatsGranularity.values().forEach { granularity ->
            val tab = tab_layout.newTab().apply {
                setText(getStringForGranularity(granularity))
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
                barchart_progress.visibility = View.VISIBLE
                listener.loadStats(tab.tag as StatsGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun populateView(
        revenueStats: Map<String, Double>,
        orderStats: Map<String, Int>,
        currencyCode: String?,
        timeframe: StatsGranularity = getActiveGranularity()
    ) {
        barchart_progress.visibility = View.GONE

        revenue_value.text = formatCurrencyAmountForDisplay(revenueStats.values.sum(), currencyCode)
        orders_value.text = orderStats.values.sum().toString()

        if (revenueStats.isEmpty()) {
            // TODO Replace with custom empty view
            chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.graph_no_data_test_color))
            chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            chart.clear()
            return
        }

        val entries = when (timeframe) {
            StatsGranularity.DAYS,
            StatsGranularity.WEEKS,
            StatsGranularity.MONTHS -> {
                revenueStats.values.mapIndexed { index, value ->
                    BarEntry((index + 1).toFloat(), value.toFloat())
                }
            }
                }
            }
            StatsGranularity.YEARS -> TODO()
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(context, R.color.graph_data_color)
            setDrawValues(false)
            isHighlightEnabled = false
        }

        with (chart) {
            with (xAxis) {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time

                setLabelCount(2, true) // Only show first and last date

                valueFormatter = when (timeframe) {
                    StatsGranularity.DAYS -> IAxisValueFormatter { value, axis ->
                        when (value) {
                            axis.mEntries.first() -> {
                                DateUtils.getFriendlyMonthDayString(revenueStats.keys.first())
                            }
                            axis.mEntries.max() -> {
                                DateUtils.getFriendlyMonthDayString(revenueStats.keys.last())
                            }
                            else -> ""
                        }
                    }
                    StatsGranularity.WEEKS -> IAxisValueFormatter { value, axis ->
                        when (value) {
                            axis.mEntries.first() -> {
                                DateUtils.getFriendlyMonthDayStringForWeek(revenueStats.keys.first())
                            }
                            axis.mEntries.max() -> {
                                DateUtils.getFriendlyMonthDayStringForWeek(revenueStats.keys.last())
                            }
                            else -> ""
                        }
                    }
                    StatsGranularity.MONTHS -> IAxisValueFormatter { value, axis ->
                        when (value) {
                            axis.mEntries.first() -> {
                                DateUtils.getFriendlyMonthString(revenueStats.keys.first())
                            }
                            axis.mEntries.max() -> {
                                DateUtils.getFriendlyMonthString(revenueStats.keys.last())
                            }
                            else -> ""
                        }
                    }
                    StatsGranularity.YEARS -> TODO()
                }
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
                    formatCurrencyAmountForDisplay(value.toDouble(), currencyCode)
                }
            }

            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false

            // This disables pinch to zoom and swiping through the zoomed graph
            // We can reenable it, but we'll probably want to disable pull-to-refresh inside the graph view
            setTouchEnabled(false)

            // Set the data after everything is configured to prevent a premature redrawing of the chart
            data = BarData(dataSet)

            // Format the X-axis value range according to the current timeframe and given values
            formatXAxisValueRange(this, timeframe, revenueStats.keys)

            invalidate() // Draw the graph
        }
    }

    fun getActiveGranularity(): StatsGranularity {
        return tab_layout.getTabAt(tab_layout.selectedTabPosition)?.let {
            it.tag as StatsGranularity
        } ?: StatsGranularity.DAYS
    }

    // TODO For certain currencies/locales, replace the thousands mark with k
    private fun formatCurrencyAmountForDisplay(amount: Double, currencyCode: String?): String {
        return amount.takeIf { it > 0 }?.let {
            CurrencyUtils.currencyStringRounded(context, amount, currencyCode ?: "")
        } ?: ""
    }

    private fun formatXAxisValueRange(chart: BarChart, granularity: StatsGranularity, dateList: Set<String>) {
        with (chart.xAxis) {
            resetAxisMinimum()
            resetAxisMaximum()
            calculate(chart.data.xMin, chart.data.xMax)
        }

        when (granularity) {
            StatsGranularity.DAYS -> chart.setVisibleXRangeMinimum(30F)
            StatsGranularity.WEEKS -> chart.setVisibleXRangeMinimum(17F)
            StatsGranularity.MONTHS -> chart.setVisibleXRangeMinimum(12F)
            StatsGranularity.YEARS -> TODO()
        }
    }

    @StringRes
    private fun getStringForGranularity(timeframe: StatsGranularity): Int {
        return when (timeframe) {
            StatsGranularity.DAYS -> R.string.dashboard_stats_granularity_days
            StatsGranularity.WEEKS -> R.string.dashboard_stats_granularity_weeks
            StatsGranularity.MONTHS -> R.string.dashboard_stats_granularity_months
            StatsGranularity.YEARS -> R.string.dashboard_stats_granularity_years
        }
    }
}
