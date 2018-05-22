package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
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

class DashboardStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    enum class StatsTimeframe { THIS_WEEK, THIS_MONTH, THIS_YEAR, YEARS }

    fun initView() {
        barchart_progress.visibility = View.VISIBLE
        // TODO: Init tab layout with StatsTimeframe values
    }

    fun populateView(
        revenueStats: Map<String, Double>,
        orderStats: Map<String, Int>,
        currencyCode: String?,
        timeframe: StatsTimeframe = getActiveTimeframe()
    ) {
        barchart_progress.visibility = View.GONE

        revenue_value.text = formatCurrencyAmountForDisplay(revenueStats.values.sum(), currencyCode)
        orders_value.text = orderStats.values.sum().toString()

        if (revenueStats.isEmpty()) {
            // TODO Replace with custom empty view
            chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.default_text_color))
            chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            chart.clear()
            return
        }

        val entries = when (timeframe) {
            StatsTimeframe.THIS_WEEK -> TODO()
            StatsTimeframe.THIS_MONTH -> {
                revenueStats.map({
                    BarEntry(it.key.substringAfterLast("-").toFloat(), it.value.toFloat())
                })
            }
            StatsTimeframe.THIS_YEAR -> TODO()
            StatsTimeframe.YEARS -> TODO()
        }

        val dataSet = BarDataSet(entries, "").apply {
            color = ContextCompat.getColor(context, R.color.wc_purple)
            setDrawValues(false)
        }

        with (chart) {
            data = BarData(dataSet)

            with (xAxis) {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time
            }

            // Format the X-axis value range according to the current timeframe and given values
            formatXAxisValueRange(this, timeframe, revenueStats.keys)

            with (axisLeft) {
                setDrawAxisLine(false)

                setDrawGridLines(true)
                enableGridDashedLine(10F, 10F, 0F)
                gridColor = ContextCompat.getColor(context, R.color.wc_border_color)

                setDrawZeroLine(true)
                zeroLineWidth = 1F
                zeroLineColor = ContextCompat.getColor(context, R.color.wc_border_color)

                axisMinimum = 0F

                valueFormatter = IAxisValueFormatter { value, _ ->
                    formatCurrencyAmountForDisplay(value.toDouble(), currencyCode)
                }
            }

            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false

            invalidate() // Draw the graph
        }
    }

    fun getActiveTimeframe(): StatsTimeframe {
        // TODO: Return state of timeframe selector
        return StatsTimeframe.THIS_MONTH
    }

    // TODO For certain currencies/locales, replace the thousands mark with k
    private fun formatCurrencyAmountForDisplay(amount: Double, currencyCode: String?) =
            CurrencyUtils.currencyStringRounded(context, amount, currencyCode ?: "")

    private fun formatXAxisValueRange(chart: BarChart, timeframe: StatsTimeframe, dateList: Set<String>) {
        when (timeframe) {
            StatsTimeframe.THIS_WEEK -> TODO()
            StatsTimeframe.THIS_MONTH -> {
                // Expand the x-axis up to the total days in the current month
                // (regardless of the current day of the month)
                val daysInMonth = DateUtils.getNumberOfDaysInMonth(dateList.first())
                chart.setVisibleXRangeMinimum(daysInMonth.toFloat())
            }
            StatsTimeframe.THIS_YEAR -> TODO()
            StatsTimeframe.YEARS -> TODO()
        }
    }
}
