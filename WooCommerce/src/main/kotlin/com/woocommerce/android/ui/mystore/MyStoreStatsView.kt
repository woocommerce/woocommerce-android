package com.woocommerce.android.ui.mystore

import android.content.Context
import android.os.Handler
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis.XAxisPosition
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.MyStoreStatsBinding
import com.woocommerce.android.extensions.formatDateToYearMonth
import com.woocommerce.android.extensions.formatToDateOnly
import com.woocommerce.android.extensions.formatToMonthDateOnly
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreFragment.Companion.DEFAULT_STATS_GRANULARITY
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FormatCurrencyRounded
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.SkeletonView
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.DateTimeUtils
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import kotlin.math.round

class MyStoreStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr), OnChartValueSelectedListener, BarChartGestureListener {
    private val binding = MyStoreStatsBinding.inflate(LayoutInflater.from(ctx), this)
    
    companion object {
        private const val UPDATE_DELAY_TIME_MS = 60 * 1000L
    }

    private lateinit var activeGranularity: StatsGranularity
    private var listener: MyStoreStatsListener? = null

    private lateinit var selectedSite: SelectedSite
    private lateinit var formatCurrencyForDisplay: FormatCurrencyRounded

    private var revenueStatsModel: WCRevenueStatsModel? = null
    private var chartRevenueStats = mapOf<String, Double>()
    private var chartOrderStats = mapOf<String, Long>()
    private var chartVisitorStats = mapOf<String, Int>()
    private var chartCurrencyCode: String? = null

    private var skeletonView = SkeletonView()

    private lateinit var lastUpdatedRunnable: Runnable
    private var lastUpdatedHandler: Handler? = null
    private var lastUpdated: Date? = null

    private var isRequestingStats = false
        set(value) {
            // if we're requesting chart data we clear the existing data so it doesn't continue
            // to appear, and we remove the chart's empty string so it doesn't briefly show
            // up before the chart data is added once the request completes
            if (value) {
                clearLabelValues()
                binding.chart.setNoDataText(null)
                binding.chart.clear()
            } else {
                // TODO: add a custom empty view
                binding.chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            }
            field = value
        }

    private val fadeHandler = Handler()

    private val visitorsLayout
        get() = binding.root.findViewById<ViewGroup>(R.id.visitors_layout)

    private val visitorsValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.visitors_value)
    
    private val revenueValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.revenue_value)

    private val ordersValue
        get() = binding.root.findViewById<MaterialTextView>(R.id.orders_value)

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: MyStoreStatsListener,
        selectedSite: SelectedSite,
        formatCurrencyForDisplay: FormatCurrencyRounded
    ) {
        this.listener = listener
        this.selectedSite = selectedSite
        this.activeGranularity = period
        this.formatCurrencyForDisplay = formatCurrencyForDisplay

        initChart()

        lastUpdatedHandler = Handler()
        lastUpdatedRunnable = Runnable {
            updateRecencyMessage()
            lastUpdatedHandler?.postDelayed(lastUpdatedRunnable,
                    UPDATE_DELAY_TIME_MS
            )
        }
    }

    fun removeListener() {
        listener = null
    }

    fun loadDashboardStats(granularity: StatsGranularity) {
        this.activeGranularity = granularity
        // Track range change
        AnalyticsTracker.track(
                Stat.DASHBOARD_MAIN_STATS_DATE,
                mapOf(AnalyticsTracker.KEY_RANGE to granularity.toString().toLowerCase(Locale.ROOT)))

        isRequestingStats = true
        listener?.onRequestLoadStats(granularity)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == View.VISIBLE) {
            updateRecencyMessage()
        } else {
            lastUpdatedHandler?.removeCallbacks(lastUpdatedRunnable)
        }
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            // inflate the skeleton view and adjust the bar widths based on the granularity
            val inflater = LayoutInflater.from(context)
            val skeleton = inflater.inflate(R.layout.skeleton_dashboard_stats, binding.chartContainer, false) as ViewGroup
            val barWidth = getSkeletonBarWidth()
            for (i in 0 until skeleton.childCount) {
                skeleton.getChildAt(i).layoutParams.width = barWidth
            }

            skeletonView.show(binding.chartContainer, skeleton, delayed = true)
            binding.dashboardRecencyText.text = null
        } else {
            skeletonView.hide()
        }
    }

    private fun getSkeletonBarWidth(): Int {
        val resId = when (activeGranularity) {
            StatsGranularity.DAYS -> R.dimen.skeleton_bar_chart_bar_width_days
            StatsGranularity.WEEKS -> R.dimen.skeleton_bar_chart_bar_width_weeks
            StatsGranularity.MONTHS -> R.dimen.skeleton_bar_chart_bar_width_months
            StatsGranularity.YEARS -> R.dimen.skeleton_bar_chart_bar_width_years
        }
        return context.resources.getDimensionPixelSize(resId)
    }

    private fun getBarLabelCount(): Int {
        val resId = when (activeGranularity) {
            StatsGranularity.DAYS -> R.integer.stats_label_count_days
            StatsGranularity.WEEKS -> R.integer.stats_label_count_weeks
            StatsGranularity.MONTHS -> R.integer.stats_label_count_months
            StatsGranularity.YEARS -> R.integer.stats_label_count_years
        }
        val chartRevenueStatsSize = chartRevenueStats.keys.size
        val barLabelCount = context.resources.getInteger(resId)
        return if (chartRevenueStatsSize < barLabelCount) {
            chartRevenueStatsSize
        } else barLabelCount
    }

    /**
     * One-time chart initialization with settings common to all granularities.
     */
    private fun initChart() {
        with(binding.chart) {
            with(xAxis) {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)

                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f
            }

            axisRight.isEnabled = false
            with(axisLeft) {
                setDrawTopYLabelEntry(true)
                setDrawAxisLine(false)
                setDrawGridLines(true)
                gridColor = ContextCompat.getColor(context, R.color.graph_grid_color)
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)

                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f
            }

            description.isEnabled = false
            legend.isEnabled = false

            // touch has to be enabled in order to show a marker when a bar is tapped, but we don't want
            // pinch/zoom, drag, or scaling to be enabled
            setTouchEnabled(true)
            setPinchZoom(false)
            isScaleXEnabled = false
            isScaleYEnabled = false
            isDragEnabled = true

            setNoDataTextColor(ContextCompat.getColor(context, R.color.graph_no_data_text_color))
            getPaint(Chart.PAINT_INFO).textSize = context.resources.getDimension(R.dimen.text_minor_125)
        }
        binding.chart.setOnChartValueSelectedListener(this)
        binding.chart.onChartGestureListener = this
    }
    
    /**
     * Called when nothing has been selected or an "un-select" has been made.
     */
    override fun onNothingSelected() {
        // update the total values of the chart here
        updateChartView()
        if (visitorsLayout.visibility == View.GONE) {
            visitorsLayout.visibility = View.VISIBLE
        }
        fadeInLabelValue(visitorsValue, chartVisitorStats.values.sum().toString())

        // update date bar when unselected
        listener?.onChartValueUnSelected(revenueStatsModel, activeGranularity)
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        val barEntry = e as BarEntry

        // display the revenue for this entry
        val formattedRevenue = getFormattedRevenueValue(barEntry.y.toDouble())
        revenueValue.text = formattedRevenue

        // display the order count for this entry
        val date = getDateFromIndex(barEntry.x.toInt())
        val value = chartOrderStats[date]?.toInt() ?: 0
        ordersValue.text = value.toString()

        // display the visitor count for this entry only if the text is NOT empty
        val visitorValue = getFormattedVisitorValue(date)
        if (visitorValue.isEmpty()) {
            visitorsLayout.visibility = View.GONE
        } else {
            visitorsLayout.visibility = View.VISIBLE
            visitorsValue.text = visitorValue
        }

        // update the date bar
        listener?.onChartValueSelected(date, activeGranularity)
    }

    /**
     * Method called when a touch-gesture has ended on the chart (ACTION_UP, ACTION_CANCEL)
     * If the touch gesture has ended, then display the entire chart data again
     */
    override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartGesture?) {
        if (lastPerformedGesture == ChartGesture.DRAG || lastPerformedGesture == ChartGesture.FLING) {
            onNothingSelected()
        }
    }

    /**
     * removes the highlighted value, which in turn removes the marker view
     */
    private fun hideMarker() {
        binding.chart.highlightValue(null)
    }

    fun updateView(revenueStatsModel: WCRevenueStatsModel?, currencyCode: String?) {
        this.revenueStatsModel = revenueStatsModel
        chartCurrencyCode = currencyCode

        // There are times when the stats v4 api returns no grossRevenue or ordersCount for a site
        // https://github.com/woocommerce/woocommerce-android/issues/1455#issuecomment-540401646
        this.chartRevenueStats = revenueStatsModel?.getIntervalList()?.map {
            it.interval!! to (it.subtotals?.totalSales ?: 0.0)
        }?.toMap() ?: mapOf()

        this.chartOrderStats = revenueStatsModel?.getIntervalList()?.map {
            it.interval!! to (it.subtotals?.ordersCount ?: 0)
        }?.toMap() ?: mapOf()

        updateChartView()
    }

    fun showErrorView(show: Boolean) {
        isRequestingStats = false
        binding.dashboardStatsError.isVisible = show
        binding.chart.isVisible = !show
    }

    fun showVisitorStats(visitorStats: Map<String, Int>) {
        chartVisitorStats = getFormattedVisitorStats(visitorStats)
        if (visitorsLayout.visibility == View.GONE) {
            WooAnimUtils.fadeIn(visitorsLayout)
        }
        fadeInLabelValue(visitorsValue, visitorStats.values.sum().toString())
    }

    fun showVisitorStatsError() {
        if (visitorsLayout.visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(visitorsLayout)
        }
    }

    fun clearLabelValues() {
        val color = ContextCompat.getColor(context, R.color.skeleton_color)
        visitorsValue.setTextColor(color)
        revenueValue.setTextColor(color)
        ordersValue.setTextColor(color)

        visitorsValue.setText(R.string.emdash)
        revenueValue.setText(R.string.emdash)
        ordersValue.setText(R.string.emdash)
    }

    fun clearChartData() {
        binding.chart.data?.clearValues()
    }

    private fun updateChartView() {
        val wasEmpty = binding.chart.barData?.let { it.dataSetCount == 0 } ?: true

        val totalModel = revenueStatsModel?.parseTotal()
        val grossRevenue = totalModel?.totalSales ?: 0.0
        val revenue = formatCurrencyForDisplay(grossRevenue, chartCurrencyCode.orEmpty())

        val orderCount = totalModel?.ordersCount ?: 0
        val orders = orderCount.toString()

        fadeInLabelValue(revenueValue, revenue)
        fadeInLabelValue(ordersValue, orders)

        if (chartRevenueStats.isEmpty() || totalModel?.totalSales?.toInt() == 0) {
            clearLastUpdated()
            isRequestingStats = false
            return
        }

        val barColors = ArrayList<Int>()
        val normalColor = ContextCompat.getColor(context, R.color.graph_data_color)
        for (entry in chartRevenueStats) {
            barColors.add(normalColor)
        }

        val dataSet = generateBarDataSet(chartRevenueStats).apply {
            colors = barColors
            setDrawValues(false)
            isHighlightEnabled = true
            highLightColor = ContextCompat.getColor(context, R.color.graph_highlight_color)
        }

        // determine the min revenue so we can set the min value for the left axis, which should be zero unless
        // the stats contain any negative revenue
        var minRevenue = 0f
        for (value in dataSet.values) {
            if (value.y < minRevenue) minRevenue = value.y
        }

        val duration = context.resources.getInteger(android.R.integer.config_shortAnimTime)
        with(binding.chart) {
            data = BarData(dataSet)
            if (wasEmpty) {
                animateY(duration)
            }
            with(xAxis) {
                // Added axis minimum offset & axis max offset in order to align the bar chart with the x-axis labels
                // Related fix: https://github.com/PhilJay/MPAndroidChart/issues/2566
                val axisValue = 0.5f
                axisMinimum = data.xMin - axisValue
                axisMaximum = data.xMax + axisValue
                labelCount = getBarLabelCount()
                setCenterAxisLabels(false)
                valueFormatter = StartEndDateAxisFormatter()
                yOffset = if (minRevenue < 0f) {
                    resources.getDimension(R.dimen.chart_axis_bottom_padding)
                } else 0f
            }
            with(axisLeft) {
                if (minRevenue < 0f) {
                    setDrawZeroLine(true)
                    zeroLineColor = ContextCompat.getColor(context, R.color.divider_color)
                    setLabelCount(3, true)
                } else labelCount = 3
                valueFormatter = RevenueAxisFormatter()
            }
        }

        hideMarker()
        resetLastUpdated()
        isRequestingStats = false
    }

    private fun getFormattedRevenueValue(revenue: Double) =
            formatCurrencyForDisplay(revenue, chartCurrencyCode.orEmpty())

    private fun getDateFromIndex(dateIndex: Int) = chartRevenueStats.keys.elementAt(dateIndex - 1)

    private fun getFormattedVisitorValue(date: String) =
            if (activeGranularity == StatsGranularity.DAYS) "" else
                chartVisitorStats[date]?.toString() ?: "0"

    /**
     * Method to format the incoming visitor stats data
     * The [visitorStats] map keys are in a different date format compared to [chartRevenueStats] map date format.
     * To add scrubbing interaction, we are converting the [visitorStats] date format to [chartRevenueStats] date format
     * [StatsGranularity.WEEKS] format is the same for both
     * [StatsGranularity.MONTHS] format is the same for both
     * [StatsGranularity.YEARS] visitor stats date format (yyyy-MM-dd) to yyyy-MM
     * [StatsGranularity.DAYS] format is the same for both
     */
    private fun getFormattedVisitorStats(visitorStats: Map<String, Int>): Map<String, Int> {
        return if (activeGranularity == StatsGranularity.YEARS) visitorStats.mapKeys {
            it.key.formatDateToYearMonth()
        } else visitorStats
    }

    private fun fadeInLabelValue(view: TextView, value: String) {
        // do nothing if value hasn't changed
        if (view.text.toString() == value) {
            return
        }

        // fade out the current value
        val duration = Duration.SHORT
        WooAnimUtils.fadeOut(view, duration, View.INVISIBLE)

        // fade in the new value after fade out finishes
        val delay = duration.toMillis(context) + 100
        fadeHandler.postDelayed({
            val color = ContextCompat.getColor(context, R.color.color_on_surface_high)
            view.setTextColor(color)
            view.text = value
            WooAnimUtils.fadeIn(view, duration)
        }, delay)
    }

    private fun generateBarDataSet(revenueStats: Map<String, Double>): BarDataSet {
        chartRevenueStats = revenueStats
        val barEntries = chartRevenueStats.values.mapIndexed { index, value ->
            BarEntry((index + 1).toFloat(), value.toFloat())
        }
        return BarDataSet(barEntries, "")
    }

    @StringRes
    fun getStringForGranularity(timeframe: StatsGranularity): Int {
        return when (timeframe) {
            StatsGranularity.DAYS -> R.string.today
            StatsGranularity.WEEKS -> R.string.this_week
            StatsGranularity.MONTHS -> R.string.this_month
            StatsGranularity.YEARS -> R.string.this_year
        }
    }

    private fun clearLastUpdated() {
        lastUpdated = null
        updateRecencyMessage()
    }

    private fun resetLastUpdated() {
        lastUpdated = Date()
        updateRecencyMessage()
    }

    private fun updateRecencyMessage() {
        binding.dashboardRecencyText.text = getRecencyMessage()
        lastUpdatedHandler?.removeCallbacks(lastUpdatedRunnable)

        if (lastUpdated != null) {
            lastUpdatedHandler?.postDelayed(lastUpdatedRunnable,
                    UPDATE_DELAY_TIME_MS
            )
        }
    }

    private fun getEntryValue(dateString: String): String {
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils().getShortHourString(dateString)
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> dateString.formatToMonthDateOnly()
            StatsGranularity.YEARS -> DateUtils().getShortMonthString(dateString)
        }
    }

    /**
     * Returns the text to use for the "recency message" which tells the user when stats were last updated
     */
    private fun getRecencyMessage(): String? {
        if (lastUpdated == null) {
            return null
        }

        val now = Date()

        // up to 2 minutes -> "Updated moments ago"
        val minutes = DateTimeUtils.minutesBetween(now, lastUpdated)
        if (minutes <= 2) {
            return context.getString(R.string.dashboard_stats_updated_now)
        }

        // up to 59 minutes -> "Updated 5 minutes ago"
        if (minutes <= 59) {
            return String.format(context.getString(R.string.dashboard_stats_updated_minutes), minutes)
        }

        // 1 hour -> "Updated 1 hour ago"
        val hours = DateTimeUtils.hoursBetween(now, lastUpdated)
        if (hours == 1) {
            return context.getString(R.string.dashboard_stats_updated_one_hour)
        }

        // up to 23 hours -> "Updated 5 hours ago"
        if (hours <= 23) {
            return String.format(context.getString(R.string.dashboard_stats_updated_hours), hours)
        }

        // up to 47 hours -> "Updated 1 day ago"
        if (hours <= 47) {
            return context.getString(R.string.dashboard_stats_updated_one_day)
        }

        // otherwise date & time
        val dateStr = DateFormat.getDateFormat(context).format(lastUpdated)
        val timeStr = DateFormat.getTimeFormat(context).format(lastUpdated)
        return String.format(context.getString(R.string.dashboard_stats_updated_date_time), "$dateStr $timeStr")
    }

    private inner class StartEndDateAxisFormatter : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            var index = round(value).toInt() - 1
            index = if (index == -1) index + 1 else index
            return if (index > -1 && index < chartRevenueStats.keys.size) {
                // if this is the first entry in the chart, then display the month as well as the date
                // for weekly and monthly stats
                val dateString = chartRevenueStats.keys.elementAt(index)
                if (value == axis.mEntries.first()) {
                    getEntryValue(dateString)
                } else {
                    getLabelValue(dateString)
                }
            } else ""
        }

        /**
         * Displays the x-axis labels in the following format based on [StatsGranularity]
         * [StatsGranularity.DAYS] would be 7am, 8am, 9am
         * [StatsGranularity.WEEKS] would be Aug 31, Sept 1, 2, 3
         * [StatsGranularity.MONTHS] would be Aug 1, 2, 3
         * [StatsGranularity.YEARS] would be Jan, Feb, Mar
         */
        private fun getLabelValue(dateString: String): String {
            return when (activeGranularity) {
                StatsGranularity.DAYS -> DateUtils().getShortHourString(dateString)
                StatsGranularity.WEEKS -> getWeekLabelValue(dateString)
                StatsGranularity.MONTHS -> dateString.formatToDateOnly()
                StatsGranularity.YEARS -> DateUtils().getShortMonthString(dateString)
            }
        }

        /**
         * Method returns the formatted date for the [StatsGranularity.WEEKS] tab,
         * if the date string is the first day of the month. i.e. date is equal to 1,
         * then the formatted date would be `MM-d` format.
         * Otherwise the formatted date would be `d` format
         */
        private fun getWeekLabelValue(dateString: String): String {
            val formattedDateString = dateString.formatToDateOnly()
            return if (formattedDateString == "1") {
                dateString.formatToMonthDateOnly()
            } else {
                dateString.formatToDateOnly()
            }
        }
    }

    /**
     * Custom AxisFormatter for the Y-axis which only displays 3 labels:
     * the maximum, minimum and 0 value labels
     */
    private inner class RevenueAxisFormatter : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase): String {
            return getFormattedRevenueValue(value.toDouble())
        }
    }
}
