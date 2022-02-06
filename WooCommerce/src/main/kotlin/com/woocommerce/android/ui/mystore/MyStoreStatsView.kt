package com.woocommerce.android.ui.mystore

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.github.mikephil.charting.charts.Chart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.MarkerImage
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.MyStoreStatsBinding
import com.woocommerce.android.extensions.*
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.mystore.MyStoreFragment.Companion.DEFAULT_STATS_GRANULARITY
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.util.roundToTheNextPowerOfTen
import com.woocommerce.android.widgets.SkeletonView
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.DisplayUtils
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.round

class MyStoreStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr), OnChartValueSelectedListener, BarChartGestureListener {
    private val binding = MyStoreStatsBinding.inflate(LayoutInflater.from(ctx), this)

    companion object {
        private const val LINE_CHART_DOT_OFFSET = -5
    }

    private lateinit var activeGranularity: StatsGranularity

    private lateinit var selectedSite: SelectedSite
    private lateinit var dateUtils: DateUtils
    private lateinit var currencyFormatter: CurrencyFormatter

    private var revenueStatsModel: RevenueStatsUiModel? = null
    private var chartRevenueStats = mapOf<String, Double>()
    private var chartOrderStats = mapOf<String, Long>()
    private var chartVisitorStats = mapOf<String, Int>()

    private var skeletonView = SkeletonView()

    private var isRequestingStats = false
        set(value) {
            // if we're requesting chart data we clear the existing data so it doesn't continue
            // to appear, and we remove the chart's empty string so it doesn't briefly show
            // up before the chart data is added once the request completes
            if (value) {
                clearStatsHeaderValues()
                binding.chart.setNoDataText(null)
                binding.chart.clear()
            } else {
                // TODO: add a custom empty view
                binding.chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            }
            field = value
        }

    private val fadeHandler = Handler(Looper.getMainLooper())

    private val statsDateValue
        get() = binding.statsViewRow.statsDateTextView

    private val revenueValue
        get() = binding.statsViewRow.totalRevenueTextView

    private val ordersValue
        get() = binding.statsViewRow.ordersValueTextView

    private val visitorsValue
        get() = binding.statsViewRow.visitorsValueTextview

    private val conversionValue
        get() = binding.statsViewRow.conversionValueTextView

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        selectedSite: SelectedSite,
        dateUtils: DateUtils,
        currencyFormatter: CurrencyFormatter
    ) {
        this.selectedSite = selectedSite
        this.activeGranularity = period
        this.dateUtils = dateUtils
        this.currencyFormatter = currencyFormatter

        initChart()

        visitorsValue.addTextChangedListener {
            updateConversionRate()
        }

        ordersValue.addTextChangedListener {
            updateConversionRate()
        }
    }

    fun loadDashboardStats(granularity: StatsGranularity) {
        this.activeGranularity = granularity
        // Track range change
        AnalyticsTracker.track(
            Stat.DASHBOARD_MAIN_STATS_DATE,
            mapOf(AnalyticsTracker.KEY_RANGE to granularity.toString().lowercase())
        )
        isRequestingStats = true
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(
                binding.myStoreStatsLinearLayout,
                R.layout.skeleton_dashboard_stats,
                delayed = true
            )
        } else {
            skeletonView.hide()
        }
    }

    private fun getChartXAxisLabelCount(): Int {
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
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f // Don't break x axis values down further than 1 unit of time
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)
                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f
            }
            with(axisLeft) {
                setLabelCount(3, true)
                valueFormatter = RevenueAxisFormatter()
                setDrawGridLines(true)
                gridLineWidth = 1f
                gridColor = ContextCompat.getColor(context, R.color.graph_grid_color)
                setDrawAxisLine(false)
                textColor = ContextCompat.getColor(context, R.color.graph_label_color)
                // Couldn't use the dimension resource here due to the way this component is written :/
                textSize = 10f
            }
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false

            // touch has to be enabled in order to show a marker when dragging across the line chart
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
        binding.chart.highlightValue(null)
        updateChartView()
        visitorsValue.isVisible = true
        binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = false
        fadeInLabelValue(visitorsValue, chartVisitorStats.values.sum().toString())
        updateDate(revenueStatsModel, activeGranularity)
        updateColorForStatsHeaderValues(R.color.color_on_surface_high)
    }

    private fun updateColorForStatsHeaderValues(@ColorRes colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        revenueValue.setTextColor(color)
        ordersValue.setTextColor(color)
        visitorsValue.setTextColor(color)
        conversionValue.setTextColor(color)
    }

    /**
     * Method to update the date value for a given [revenueStatsModel] based on the [granularity]
     * This is used to display the date bar when the **stats tab is loaded**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08
     * [StatsGranularity.WEEKS] would be Aug 4 - Aug 08
     * [StatsGranularity.MONTHS] would be August
     * [StatsGranularity.YEARS] would be 2019
     */
    private fun updateDate(
        revenueStats: RevenueStatsUiModel?,
        granularity: StatsGranularity
    ) {
        if (revenueStats?.intervalList.isNullOrEmpty()) {
            statsDateValue.visibility = View.GONE
        } else {
            val startInterval = revenueStats?.intervalList?.first()?.interval
            val startDate = startInterval?.let { getDateValue(it, granularity) }

            val dateRangeString = when (granularity) {
                StatsGranularity.WEEKS -> {
                    val endInterval = revenueStats?.intervalList?.last()?.interval
                    val endDate = endInterval?.let { getDateValue(it, granularity) }
                    String.format(Locale.getDefault(), "%s – %s", startDate, endDate)
                }
                else -> {
                    startDate
                }
            }
            statsDateValue.visibility = View.VISIBLE
            statsDateValue.text = dateRangeString
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
            StatsGranularity.DAYS -> dateUtils.getDayMonthDateString(dateString).orEmpty()
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> dateUtils.getMonthString(dateString).orEmpty()
            StatsGranularity.YEARS -> dateUtils.getYearString(dateString).orEmpty()
        }
    }

    override fun onValueSelected(entry: Entry?, h: Highlight?) {
        if (entry == null) return
        // display the revenue for this entry
        val formattedRevenue = getFormattedRevenueValue(entry.y.toDouble())
        revenueValue.text = formattedRevenue

        // display the order count for this entry
        val date = getDateFromIndex(entry.x.toInt())
        val orderCount = chartOrderStats[date]?.toInt() ?: 0
        ordersValue.text = orderCount.toString()
        updateVisitorsValue(date)
        updateConversionRate()
        updateDateOnScrubbing(date, activeGranularity)
        updateColorForStatsHeaderValues(R.color.color_secondary)
    }

    private fun updateVisitorsValue(date: String) {
        if (activeGranularity == StatsGranularity.DAYS) {
            visitorsValue.isVisible = false
            visitorsValue.setText(R.string.emdash)
            binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = true
        } else {
            visitorsValue.isVisible = true
            binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = false
            visitorsValue.text = chartVisitorStats[date]?.toString() ?: "0"
        }
    }

    /**
     * Method to update the date value for a given [dateString] based on the [activeGranularity]
     * This is used to display the date bar when the **scrubbing interaction is taking place**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08›7am
     * [StatsGranularity.WEEKS] would be Aug 08
     * [StatsGranularity.MONTHS] would be August›08
     * [StatsGranularity.YEARS] would be 2019›August
     */
    private fun updateDateOnScrubbing(dateString: String, activeGranularity: StatsGranularity) {
        statsDateValue.text = when (activeGranularity) {
            StatsGranularity.DAYS -> dateString.formatDateToFriendlyDayHour()
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> dateString.formatDateToFriendlyLongMonthDate()
            StatsGranularity.YEARS -> dateString.formatDateToFriendlyLongMonthYear()
        }
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

    fun updateView(revenueStatsModel: RevenueStatsUiModel?) {
        updateDate(revenueStatsModel, activeGranularity)
        this.revenueStatsModel = revenueStatsModel

        // There are times when the stats v4 api returns no grossRevenue or ordersCount for a site
        // https://github.com/woocommerce/woocommerce-android/issues/1455#issuecomment-540401646
        this.chartRevenueStats = revenueStatsModel?.intervalList?.associate {
            it.interval!! to (it.sales ?: 0.0)
        } ?: mapOf()

        this.chartOrderStats = revenueStatsModel?.intervalList?.associate {
            it.interval!! to (it.ordersCount ?: 0)
        } ?: mapOf()

        updateChartView()
    }

    fun showErrorView(show: Boolean) {
        isRequestingStats = false
        binding.dashboardStatsError.isVisible = show
        binding.chart.isVisible = !show
    }

    fun showVisitorStats(visitorStats: Map<String, Int>) {
        chartVisitorStats = getFormattedVisitorStats(visitorStats)
        // Make sure the empty view is hidden
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = false

        val totalVisitors = visitorStats.values.sum()
        fadeInLabelValue(visitorsValue, totalVisitors.toString())
    }

    fun showVisitorStatsError() {
        binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = true
        binding.statsViewRow.jetpackIconImageView.isVisible = false
        binding.statsViewRow.visitorsValueTextview.isVisible = false
    }

    fun showEmptyVisitorStatsForJetpackCP() {
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = true
        binding.statsViewRow.visitorsValueTextview.isVisible = false
    }

    private fun updateConversionRate() {
        val ordersCount = ordersValue.text.toString().toIntOrNull()
        val visitorsCount = visitorsValue.text.toString().toIntOrNull()

        if (visitorsCount == null || ordersCount == null) {
            conversionValue.isVisible = false
            binding.statsViewRow.emptyConversionRateIndicator.isVisible = true
            return
        }

        val conversionRateDisplayValue = when (visitorsCount) {
            0 -> "0%"
            else -> {
                val conversionRate = (ordersCount / visitorsCount.toFloat()) * 100
                DecimalFormat("##.#").format(conversionRate) + "%"
            }
        }
        val color = ContextCompat.getColor(context, R.color.color_on_surface_high)
        conversionValue.setTextColor(color)
        binding.statsViewRow.emptyConversionRateIndicator.isVisible = false
        conversionValue.isVisible = true
        conversionValue.text = conversionRateDisplayValue
    }

    fun clearStatsHeaderValues() {
        statsDateValue.text = ""
        updateColorForStatsHeaderValues(R.color.skeleton_color)

        visitorsValue.setText(R.string.emdash)
        revenueValue.setText(R.string.emdash)
        ordersValue.setText(R.string.emdash)
        conversionValue.setText(R.string.emdash)
    }

    fun clearChartData() {
        binding.chart.data?.clearValues()
    }

    private fun updateChartView() {
        val wasEmpty = binding.chart.lineData?.let { it.dataSetCount == 0 } ?: true

        val grossRevenue = revenueStatsModel?.totalSales ?: 0.0
        val revenue = getFormattedRevenueValue(grossRevenue)

        val orderCount = revenueStatsModel?.totalOrdersCount ?: 0
        val orders = orderCount.toString()

        fadeInLabelValue(revenueValue, revenue)
        fadeInLabelValue(ordersValue, orders)

        if (chartRevenueStats.isEmpty() || revenueStatsModel?.totalSales == 0.toDouble()) {
            isRequestingStats = false
            return
        }

        val dataSet = generateLineDataSet(chartRevenueStats).apply {
            color = ContextCompat.getColor(context, R.color.color_primary)
            setDrawValues(false)
            isHighlightEnabled = true
            highLightColor = ContextCompat.getColor(context, R.color.graph_data_color)
            highlightLineWidth = 1.5f
            setDrawHorizontalHighlightIndicator(false)
            setDrawCircles(false)
            lineWidth = 2f
            if (chartRevenueStats.all { it.value <= 0 }) {
                setDrawFilled(false)
            } else {
                fillDrawable = ContextCompat.getDrawable(context, R.drawable.line_chart_fill_gradient)
                setDrawFilled(true)
            }
        }

        // determine the min revenue so we can set the min value for the left axis, which should be zero unless
        // the stats contain any negative revenue
        val minRevenue = dataSet.values.minOf { it.y }
        val maxRevenue = dataSet.values.maxOf { it.y }
        val duration = context.resources.getInteger(android.R.integer.config_shortAnimTime)
        with(binding.chart) {
            data = LineData(dataSet)
            if (wasEmpty) {
                animateY(duration)
            }
            with(xAxis) {
                labelCount = getChartXAxisLabelCount()
                valueFormatter = StartEndDateAxisFormatter()
            }
            with(axisLeft) {
                if (minRevenue < 0) {
                    setDrawZeroLine(true)
                    zeroLineColor = ContextCompat.getColor(context, R.color.divider_color)
                }
                axisMinimum = minRevenue.roundToTheNextPowerOfTen()
                axisMaximum = maxRevenue.roundToTheNextPowerOfTen()
            }
            val dot = MarkerImage(context, R.drawable.chart_highlight_dot)
            val offset = DisplayUtils.dpToPx(context, LINE_CHART_DOT_OFFSET).toFloat()
            dot.setOffset(offset, offset)
            marker = dot
        }
        isRequestingStats = false
    }

    private fun getFormattedRevenueValue(revenue: Double) =
        if (revenue == 0.0) {
            currencyFormatter.formatCurrencyRounded(revenue, revenueStatsModel?.currencyCode.orEmpty())
        } else {
            currencyFormatter
                .formatCurrency(revenue.toBigDecimal(), revenueStatsModel?.currencyCode.orEmpty())
        }

    private fun getDateFromIndex(dateIndex: Int) = chartRevenueStats.keys.elementAt(dateIndex - 1)

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
        fadeHandler.postDelayed(
            {
                val color = ContextCompat.getColor(context, R.color.color_on_surface_high)
                view.setTextColor(color)
                view.text = value
                WooAnimUtils.fadeIn(view, duration)
            },
            delay
        )
    }

    private fun generateLineDataSet(revenueStats: Map<String, Double>): LineDataSet {
        chartRevenueStats = revenueStats
        val entries = chartRevenueStats.values.mapIndexed { index, value ->
            Entry((index + 1).toFloat(), value.toFloat())
        }
        return LineDataSet(entries, "")
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

    private fun getEntryValue(dateString: String): String {
        return when (activeGranularity) {
            StatsGranularity.DAYS -> dateUtils.getShortHourString(dateString).orEmpty()
            StatsGranularity.WEEKS -> dateString.formatToMonthDateOnly()
            StatsGranularity.MONTHS -> dateString.formatToMonthDateOnly()
            StatsGranularity.YEARS -> dateUtils.getShortMonthString(dateString).orEmpty()
        }
    }

    private inner class StartEndDateAxisFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase): String {
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
                StatsGranularity.DAYS -> dateUtils.getShortHourString(dateString).orEmpty()
                StatsGranularity.WEEKS -> getWeekLabelValue(dateString)
                StatsGranularity.MONTHS -> dateString.formatToDateOnly()
                StatsGranularity.YEARS -> dateUtils.getShortMonthString(dateString).orEmpty()
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

    private inner class RevenueAxisFormatter : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase): String {
            return if (-1 < value && value < 1 && value != 0f) {
                currencyFormatter.formatCurrency(
                    value.toBigDecimal(),
                    revenueStatsModel?.currencyCode.orEmpty()
                )
            } else {
                currencyFormatter.formatCurrencyRounded(
                    value.toDouble(),
                    revenueStatsModel?.currencyCode.orEmpty()
                ).replace(".0", "")
            }
        }
    }
}
