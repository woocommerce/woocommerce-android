package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyUtils
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

        val dataSet = BarDataSet(entries, "")

        with (chart) {
            data = BarData(dataSet)

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
}
