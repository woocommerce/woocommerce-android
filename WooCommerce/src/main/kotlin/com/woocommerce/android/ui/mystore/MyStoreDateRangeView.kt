package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.databinding.MyStoreDateBarBinding
import com.woocommerce.android.extensions.formatDateToFriendlyDayHour
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthDate
import com.woocommerce.android.extensions.formatDateToFriendlyLongMonthYear
import com.woocommerce.android.extensions.formatToMonthDateOnly
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

class MyStoreDateRangeView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    private val binding = MyStoreDateBarBinding.inflate(LayoutInflater.from(ctx), this)

    fun initView() {
        clearDateRangeValues()
    }

    /**
     * Method to update the date value for a given [revenueStatsModel] based on the [granularity]
     * This is used to display the date bar when the **stats tab is loaded**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08
     * [StatsGranularity.WEEKS] would be Aug 4 - Aug 08
     * [StatsGranularity.MONTHS] would be August
     * [StatsGranularity.YEARS] would be 2019
     */
    fun updateDateRangeView(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity
    ) {
        if (revenueStatsModel?.getIntervalList().isNullOrEmpty()) {
            binding.dashboardDateRangeValue.visibility = View.GONE
        } else {
            val startInterval = revenueStatsModel?.getIntervalList()?.first()?.interval
            val startDate = startInterval?.let { getDateValue(it, granularity) }

            val dateRangeString = when (granularity) {
                StatsGranularity.WEEKS -> {
                    val endInterval = revenueStatsModel?.getIntervalList()?.last()?.interval
                    val endDate = endInterval?.let { getDateValue(it, granularity) }
                    String.format("%s – %s", startDate, endDate)
                }
                else -> {
                    startDate
                }
            }
            binding.dashboardDateRangeValue.visibility = View.VISIBLE
            binding.dashboardDateRangeValue.text = dateRangeString
        }
    }

    fun clearDateRangeValues() {
        binding.dashboardDateRangeValue.text = ""
    }

    /**
     * Method to update the date value for a given [dateString] based on the [activeGranularity]
     * This is used to display the date bar when the **scrubbing interaction is taking place**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08›7am
     * [StatsGranularity.WEEKS] would be Aug 08
     * [StatsGranularity.MONTHS] would be August›08
     * [StatsGranularity.YEARS] would be 2019›August
     */
    fun updateDateViewOnScrubbing(dateString: String, activeGranularity: StatsGranularity) {
        binding.dashboardDateRangeValue.text = when (activeGranularity) {
            StatsGranularity.DAYS -> dateString.formatDateToFriendlyDayHour()
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> dateString.formatDateToFriendlyLongMonthDate()
            StatsGranularity.YEARS -> dateString.formatDateToFriendlyLongMonthYear()
        }
    }

    /**
     * Method to get the date value for a given [dateString] based on the [activeGranularity]
     * This is used to populate the date bar when the **stats tab is loaded**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08
     * [StatsGranularity.WEEKS] would be Aug 4
     * [StatsGranularity.MONTHS] would be August
     * [StatsGranularity.YEARS] would be 2019
     */
    private fun getDateValue(
        dateString: String,
        activeGranularity: StatsGranularity
    ): String {
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils().getDayMonthDateString(dateString).orEmpty()
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> DateUtils().getMonthString(dateString).orEmpty()
            StatsGranularity.YEARS -> DateUtils().getYearString(dateString).orEmpty()
        }
    }
}
