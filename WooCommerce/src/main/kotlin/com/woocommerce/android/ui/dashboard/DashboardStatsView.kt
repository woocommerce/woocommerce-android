package com.woocommerce.android.ui.dashboard

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
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.formatDateToWeeksInYear
import com.woocommerce.android.extensions.formatDateToYear
import com.woocommerce.android.extensions.formatDateToYearMonth
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardFragment.Companion.DEFAULT_STATS_GRANULARITY
import com.woocommerce.android.ui.mystore.BarChartGestureListener
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FormatCurrencyRounded
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dashboard_main_stats_row.view.*
import kotlinx.android.synthetic.main.dashboard_stats.view.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.utils.SiteUtils
import org.wordpress.android.util.DateTimeUtils
import java.io.Serializable
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class DashboardStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr), OnChartValueSelectedListener, BarChartGestureListener {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    companion object {
        private const val UPDATE_DELAY_TIME_MS = 60 * 1000L
    }

    var tabStateStats: Serializable? = null // Save the current position of stats tab view

    val activeGranularity: StatsGranularity
        get() {
            return tab_layout.getTabAt(tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: tabStateStats?.let { it as StatsGranularity } ?: DEFAULT_STATS_GRANULARITY
        }

    private lateinit var selectedSite: SelectedSite

    private lateinit var formatCurrencyForDisplay: FormatCurrencyRounded

    private var chartRevenueStats = mapOf<String, Double>()
    private var chartOrderStats = mapOf<String, Int>()
    private var chartVisitorStats = mapOf<String, Int>()
    private var chartCurrencyCode: String? = null

    private var skeletonView = SkeletonView()

    private var lastUpdatedRunnable: Runnable? = null
    private val lastUpdatedHandler = Handler()
    private var lastUpdated: Date? = null

    private var isRequestingStats = false
        set(value) {
            // if we're requesting chart data we clear the existing data so it doesn't continue
            // to appear, and we remove the chart's empty string so it doesn't briefly show
            // up before the chart data is added once the request completes
            if (value) {
                clearLabelValues()
                clearDateRangeValues()
                chart.setNoDataText(null)
                chart.clear()
            } else {
                // TODO: add a custom empty view
                chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            }
            field = value
        }

    private val fadeHandler = Handler()

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: DashboardStatsListener,
        selectedSite: SelectedSite,
        formatCurrencyForDisplay: FormatCurrencyRounded
    ) {
        this.selectedSite = selectedSite
        this.formatCurrencyForDisplay = formatCurrencyForDisplay

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
                // Track range change
                AnalyticsTracker.track(
                        Stat.DASHBOARD_MAIN_STATS_DATE,
                        mapOf(AnalyticsTracker.KEY_RANGE to tab.tag.toString().toLowerCase(Locale.ROOT)))

                isRequestingStats = true
                listener.onRequestLoadStats(tab.tag as StatsGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        initChart()
        lastUpdatedRunnable = Runnable {
            updateRecencyMessage()
            lastUpdatedRunnable?.let { lastUpdatedHandler.postDelayed(it, UPDATE_DELAY_TIME_MS) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateRecencyMessage()
    }

    override fun onDetachedFromWindow() {
        lastUpdatedRunnable?.let { lastUpdatedHandler.removeCallbacks(it) }
        super.onDetachedFromWindow()
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            // inflate the skeleton view and adjust the bar widths based on the granularity
            val inflater = LayoutInflater.from(context)
            val skeleton = inflater.inflate(R.layout.skeleton_dashboard_stats, chart_container, false) as ViewGroup
            val barWidth = getSkeletonBarWidth()
            for (i in 0 until skeleton.childCount) {
                skeleton.getChildAt(i).layoutParams.width = barWidth
            }

            skeletonView.show(chart_container, skeleton, delayed = true)
            dashboard_recency_text.text = null
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

    /**
     * One-time chart initialization with settings common to all granularities.
     */
    private fun initChart() {
        with(chart) {
            with(xAxis) {
                position = XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time

                setLabelCount(2, true) // Only show first and last date
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)

                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f

                valueFormatter = StartEndDateAxisFormatter()
                yOffset = resources.getDimension(R.dimen.chart_axis_bottom_padding)
            }

            axisRight.isEnabled = false

            with(axisLeft) {
                setDrawZeroLine(true)
                setDrawTopYLabelEntry(true)
                setDrawAxisLine(false)
                setDrawGridLines(true)
                zeroLineColor = ContextCompat.getColor(context, R.color.graph_grid_color)
                gridColor = ContextCompat.getColor(context, R.color.graph_grid_color)
                setLabelCount(3, true)
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)

                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f

                valueFormatter = IAxisValueFormatter { value, _ ->
                    // Only use non-zero values for the axis
                    value.toDouble().takeIf { it > 0 }?.let {
                        getFormattedRevenueValue(it)
                    }.orEmpty()
                }
            }

            description.isEnabled = false
            legend.isEnabled = false

            // touch has to be enabled in order to show a marker when a bar is tapped, but we don't want
            // pinch/zoom, or scaling to be enabled
            setTouchEnabled(true)
            setPinchZoom(false)
            isScaleXEnabled = false
            isScaleYEnabled = false
            isDragEnabled = true

            setNoDataTextColor(ContextCompat.getColor(context, R.color.graph_no_data_text_color))
            getPaint(Chart.PAINT_INFO).textSize = context.resources.getDimension(R.dimen.text_minor_125)
        }

        chart.setOnChartValueSelectedListener(this)
        chart.onChartGestureListener = this
    }

    /**
     * Called when nothing has been selected or an "un-select" has been made.
     */
    override fun onNothingSelected() {
        // update the total values of the chart here
        updateChartView()
        fadeInLabelValue(visitors_value, chartVisitorStats.values.sum().toString())

        // update date bar when unselected
        updateDateRangeView()
    }

    override fun onValueSelected(e: Entry?, h: Highlight?) {
        val barEntry = e as BarEntry

        // display the revenue for this entry
        revenue_value.text = getFormattedRevenueValue(barEntry.y.toDouble())

        // display the order count for this entry
        val date = getDateFromIndex(barEntry.x.toInt())
        val orderValue = chartOrderStats.getValue(date)
        orders_value.text = orderValue.toString()

        // display the visitor count for this entry
        val visitorValue = getFormattedVisitorValue(date)
        visitors_value.text = visitorValue

        // update date bar
        dashboard_date_range_value.text = getFormattedDateValue(date)
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
        chart.highlightValue(null)
    }

    fun updateView(revenueStats: Map<String, Double>, orderStats: Map<String, Int>, currencyCode: String?) {
        chartCurrencyCode = currencyCode
        chartRevenueStats = revenueStats
        chartOrderStats = orderStats

        updateChartView()
    }

    fun showErrorView(show: Boolean) {
        isRequestingStats = false
        dashboard_stats_error.visibility = if (show) View.VISIBLE else View.GONE
        chart.visibility = if (show) View.GONE else View.VISIBLE
    }

    fun showVisitorStats(visitorStats: Map<String, Int>) {
        chartVisitorStats = getFormattedVisitorStats(visitorStats)
        if (visitors_layout.visibility == View.GONE) {
            WooAnimUtils.fadeIn(visitors_layout)
        }
        fadeInLabelValue(visitors_value, visitorStats.values.sum().toString())
    }

    fun showVisitorStatsError() {
        if (visitors_layout.visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(visitors_layout)
        }
    }

    private fun updateChartView() {
        val wasEmpty = chart.barData?.let { it.dataSetCount == 0 } ?: true

        val revenue = getFormattedRevenueValue(chartRevenueStats.values.sum())
        val orders = chartOrderStats.values.sum().toString()
        fadeInLabelValue(revenue_value, revenue)
        fadeInLabelValue(orders_value, orders)

        if (chartRevenueStats.isEmpty()) {
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

        val duration = context.resources.getInteger(android.R.integer.config_shortAnimTime)
        with(chart) {
            data = BarData(dataSet)
            if (wasEmpty) {
                animateY(duration)
            }

            with(axisLeft) {
                setLabelCount(3, true)
                valueFormatter = RevenueAxisFormatter()
            }
        }

        hideMarker()
        resetLastUpdated()

        // update the date range view only after the Bar dataset is generated
        // since we are using the [chartRevenueStats] variable to get the
        // start and end date values
        updateDateRangeView()
        isRequestingStats = false
    }

    private fun getFormattedRevenueValue(revenue: Double) =
            formatCurrencyForDisplay(revenue, chartCurrencyCode.orEmpty())

    private fun getDateFromIndex(dateIndex: Int) =
            if (activeGranularity == StatsGranularity.YEARS) dateIndex.toString() else
                chartRevenueStats.keys.elementAt(dateIndex - 1)

    /**
     * Method to format the date value displayed when scrubbing or tapping of the chart takes place.
     * [date] is formatted based on the [activeGranularity]
     * [StatsGranularity.DAYS] format would be Aug 11
     * [StatsGranularity.WEEKS] format would be Aug 11
     * [StatsGranularity.MONTHS] format would be Aug 2019
     * [StatsGranularity.YEARS] format would be 2019
     */
    private fun getFormattedDateValue(date: String): String {
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils.getShortMonthDayString(date)
            StatsGranularity.WEEKS -> DateUtils.getShortMonthDayStringForWeek(date)
            StatsGranularity.MONTHS -> DateUtils.getShortMonthYearString(date)
            StatsGranularity.YEARS -> date
        }
    }

    private fun getFormattedVisitorValue(date: String) = chartVisitorStats[date]?.toString() ?: "0"

    /**
     * Method to format the incoming visitor stats data
     * The [visitorStats] map keys are in a different date format compared to [chartRevenueStats] map date format.
     * To add scrubbing interaction, we are converting the [visitorStats] date format to [chartRevenueStats] date format
     * [StatsGranularity.WEEKS] visitor stats date format (yyyy'W'MM'W'dd) to yyyy-'W'MM
     * [StatsGranularity.MONTHS] visitor stats date format (yyyy-MM-dd) to yyyy-MM
     * [StatsGranularity.YEARS] visitor stats date format (yyyy-MM-dd) to yyyy
     * [StatsGranularity.DAYS] format is the same for both
     */
    private fun getFormattedVisitorStats(visitorStats: Map<String, Int>): Map<String, Int> {
        return visitorStats.mapKeys {
            when (activeGranularity) {
                StatsGranularity.DAYS -> it.key
                StatsGranularity.WEEKS -> it.key.formatDateToWeeksInYear()
                StatsGranularity.MONTHS -> it.key.formatDateToYearMonth()
                StatsGranularity.YEARS -> it.key.formatDateToYear()
            }
        }
    }

    /**
     * Update the date bar range with the start and end date.
     * If the start and end date are the same i.e. 2019 for YEARS, then only display
     * the date and not the range
     */
    private fun updateDateRangeView() {
        val startDate = getStartDateValue()
        val endDate = getEndDateValue()
        val dateRangeString = if (startDate == endDate) {
            startDate
        } else {
            String.format("%s – %s", startDate, endDate)
        }
        dashboard_date_range_value.text = dateRangeString
    }

    private fun clearDateRangeValues() {
        dashboard_date_range_value.text = ""
    }

    fun clearLabelValues() {
        val color = ContextCompat.getColor(context, R.color.skeleton_color)
        visitors_value.setTextColor(color)
        revenue_value.setTextColor(color)
        orders_value.setTextColor(color)

        visitors_value.setText(R.string.emdash)
        revenue_value.setText(R.string.emdash)
        orders_value.setText(R.string.emdash)
    }

    fun clearChartData() {
        chart.data?.clearValues()
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

    private fun getStartDateValue(): String {
        val dateString = chartRevenueStats.keys.first()
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils.getShortMonthDayString(dateString)
            StatsGranularity.WEEKS -> DateUtils.getShortMonthDayStringForWeek(dateString)
            StatsGranularity.MONTHS -> DateUtils.getShortMonthYearString(dateString)
            StatsGranularity.YEARS -> dateString
        }
    }

    private fun getEndDateValue(): String {
        val dateString = chartRevenueStats.keys.last()
        return when (activeGranularity) {
            StatsGranularity.DAYS -> DateUtils.getShortMonthDayString(dateString)
            StatsGranularity.WEEKS ->
                SiteUtils.getCurrentDateTimeForSite(selectedSite.get(), DateUtils.friendlyMonthDayFormat)
            StatsGranularity.MONTHS -> DateUtils.getShortMonthYearString(dateString)
            StatsGranularity.YEARS -> dateString
        }
    }

    @StringRes
    fun getStringForGranularity(timeframe: StatsGranularity): Int {
        return when (timeframe) {
            StatsGranularity.DAYS -> R.string.dashboard_stats_granularity_days
            StatsGranularity.WEEKS -> R.string.dashboard_stats_granularity_weeks
            StatsGranularity.MONTHS -> R.string.dashboard_stats_granularity_months
            StatsGranularity.YEARS -> R.string.dashboard_stats_granularity_years
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
        dashboard_recency_text.text = getRecencyMessage()
        lastUpdatedRunnable?.let { lastUpdatedHandler.removeCallbacks(it) }

        if (lastUpdated != null) {
            lastUpdatedRunnable?.let { lastUpdatedHandler.postDelayed(it, UPDATE_DELAY_TIME_MS) }
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
