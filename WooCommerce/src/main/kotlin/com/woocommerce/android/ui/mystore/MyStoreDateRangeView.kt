package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import kotlinx.android.synthetic.main.my_store_date_bar.view.*
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

class MyStoreDateRangeView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.my_store_date_bar, this)
    }

    fun initView() {
        clearDateRangeValues()
    }

    fun updateDateRangeView(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity) {
        val startInterval = revenueStatsModel?.getIntervalList()?.first()?.interval
        val startDate = startInterval?.let { getDateValue(it, granularity) }

        val dateRangeString = when (granularity) {
            StatsGranularity.WEEKS -> {
                val endInterval = revenueStatsModel?.getIntervalList()?.last()?.interval
                val endDate = endInterval?.let { getDateValue(it, granularity) }
                String.format("%s–%s", startDate, endDate)
            }
            else -> {
                startDate
            }
        }
        dashboard_date_range_value.text = dateRangeString
    }

    fun clearDateRangeValues() {
        dashboard_date_range_value.text = ""
    }

    private fun getDateValue(
        dateString: String,
        activeGranularity: StatsGranularity
    ): String {
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils.getDayMonthDateString(dateString)
            StatsGranularity.WEEKS -> DateUtils.getShortMonthDayString(dateString)
            StatsGranularity.MONTHS -> DateUtils.getMonthString(dateString)
            StatsGranularity.YEARS -> DateUtils.getYearString(dateString)
        }
    }
}
