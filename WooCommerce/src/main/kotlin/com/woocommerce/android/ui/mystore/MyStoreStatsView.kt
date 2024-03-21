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
import androidx.lifecycle.LifecycleCoroutineScope
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
import com.google.android.material.tabs.TabLayout.Tab
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_DATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_GRANULARITY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_RANGE
import com.woocommerce.android.databinding.MyStoreStatsBinding
import com.woocommerce.android.extensions.convertedFrom
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.MONTH_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.WEEK_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.YEAR_TO_DATE
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.mystore.MyStoreViewModel.Companion.SUPPORTED_RANGES_ON_MY_STORE_TAB
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.util.roundToTheNextPowerOfTen
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.DisplayUtils
import java.util.Locale
import kotlin.math.round

@FlowPreview
class MyStoreStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr), OnChartValueSelectedListener, BarChartGestureListener {
    private val binding = MyStoreStatsBinding.inflate(LayoutInflater.from(ctx), this)

    companion object {
        private const val LINE_CHART_DOT_OFFSET = -5
        private const val EVENT_EMITTER_INTERACTION_DEBOUNCE = 1000L
    }

    private lateinit var statsTimeRangeSelection: StatsTimeRangeSelection
    private lateinit var selectedSite: SelectedSite
    private lateinit var dateUtils: DateUtils
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter

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

    private val lastUpdated
        get() = binding.statsViewRow.lastUpdatedTextView

    private val conversionValue
        get() = binding.statsViewRow.conversionValueTextView

    val customRangeLabel
        get() = binding.statsViewRow.statsCustomDateRangeTextView

    private val customRangeGranularityLabel
        get() = binding.customRangeGranularityLabel

    val customRangeButton = binding.customRangeButton

    val tabLayout = binding.statsTabLayout
    private var customRangeTab: Tab? = null

    private lateinit var coroutineScope: CoroutineScope
    private val chartUserInteractions = MutableSharedFlow<Unit>()
    private lateinit var chartUserInteractionsJob: Job

    @Suppress("LongParameterList")
    fun initView(
        selectedTimeRange: StatsTimeRangeSelection,
        selectedSite: SelectedSite,
        dateUtils: DateUtils,
        currencyFormatter: CurrencyFormatter,
        usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter,
        lifecycleScope: LifecycleCoroutineScope,
        onViewAnalyticsClick: () -> Unit
    ) {
        this.selectedSite = selectedSite
        this.statsTimeRangeSelection = selectedTimeRange
        this.dateUtils = dateUtils
        this.currencyFormatter = currencyFormatter
        this.usageTracksEventEmitter = usageTracksEventEmitter
        this.coroutineScope = lifecycleScope

        initChart()

        binding.viewAnalyticsButton.setOnClickListener {
            onViewAnalyticsClick()
        }

        visitorsValue.addTextChangedListener {
            updateConversionRate()
        }

        ordersValue.addTextChangedListener {
            updateConversionRate()
        }

        chartUserInteractionsJob = coroutineScope.launch {
            chartUserInteractions
                .debounce(EVENT_EMITTER_INTERACTION_DEBOUNCE)
                .collect { usageTracksEventEmitter.interacted() }
        }

        customRangeButton.isVisible = FeatureFlag.CUSTOM_RANGE_ANALYTICS.isEnabled()
        // Create tabs and add to appbar
        SUPPORTED_RANGES_ON_MY_STORE_TAB
            .filter { it != CUSTOM }
            .forEach { rangeType ->
                val tab = tabLayout.newTab().apply {
                    setText(getStringForRangeType(rangeType))
                    tag = rangeType
                }
                tabLayout.addTab(tab)
            }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        chartUserInteractionsJob.cancel()
    }

    fun loadDashboardStats(selectedTimeRange: StatsTimeRangeSelection) {
        this.statsTimeRangeSelection = selectedTimeRange
        // Track range change
        AnalyticsTracker.track(
            AnalyticsEvent.DASHBOARD_MAIN_STATS_DATE,
            mapOf(KEY_RANGE to selectedTimeRange.selectionType.toString().lowercase())
        )
        isRequestingStats = true
        applyCustomRange(statsTimeRangeSelection)
    }

    private fun applyCustomRange(selectedTimeRange: StatsTimeRangeSelection) {
        if (selectedTimeRange.selectionType == CUSTOM) {
            addCustomRangeTabIfMissing()
            customRangeButton.isVisible = false
            customRangeLabel.isVisible = true
            customRangeGranularityLabel.isVisible = true
            customRangeLabel.text = selectedTimeRange.currentRangeDescription
            customRangeGranularityLabel.text = getStringForGranularity(selectedTimeRange.revenueStatsGranularity)
        } else {
            customRangeLabel.isVisible = false
            customRangeGranularityLabel.isVisible = false
        }
    }

    private fun addCustomRangeTabIfMissing() {
        if (customRangeTab == null) {
            val tab = tabLayout.newTab().apply {
                setText(getStringForRangeType(CUSTOM))
                tag = CUSTOM
            }
            customRangeTab = tab
            tabLayout.addTab(tab)
        }
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(
                binding.statsContent,
                R.layout.skeleton_dashboard_stats,
                delayed = true
            )
        } else {
            skeletonView.hide()
        }
    }

    private fun getChartXAxisLabelCount(): Int {
        val resId = when (statsTimeRangeSelection.selectionType) {
            TODAY -> R.integer.stats_label_count_days
            WEEK_TO_DATE -> R.integer.stats_label_count_weeks
            MONTH_TO_DATE -> R.integer.stats_label_count_months
            YEAR_TO_DATE -> R.integer.stats_label_count_years
            CUSTOM -> R.integer.stats_label_count_custom_range
            else -> error("Unsupported range value used in my store tab: ${statsTimeRangeSelection.selectionType}")
        }
        val chartRevenueStatsSize = chartRevenueStats.keys.size
        val barLabelCount = context.resources.getInteger(resId)
        return if (chartRevenueStatsSize < barLabelCount) {
            chartRevenueStatsSize
        } else {
            barLabelCount
        }
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
        updateDate(revenueStatsModel, statsTimeRangeSelection)
        updateColorForStatsHeaderValues(R.color.color_on_surface_high)
        onUserInteractionWithChart()
        if (statsTimeRangeSelection.selectionType == CUSTOM) statsDateValue.isVisible = false
    }

    private fun onUserInteractionWithChart() {
        coroutineScope.launch {
            chartUserInteractions.emit(Unit)
        }
    }

    private fun updateColorForStatsHeaderValues(@ColorRes colorRes: Int) {
        val color = ContextCompat.getColor(context, colorRes)
        revenueValue.setTextColor(color)
        ordersValue.setTextColor(color)
        visitorsValue.setTextColor(color)
        conversionValue.setTextColor(color)
    }

    /**
     * Method to update the date value for a given [revenueStatsModel] based on the [rangeType]
     * This is used to display the date bar when the **stats tab is loaded**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08
     * [StatsGranularity.WEEKS] would be Aug 4 - Aug 08
     * [StatsGranularity.MONTHS] would be August
     * [StatsGranularity.YEARS] would be 2019
     */
    private fun updateDate(
        revenueStats: RevenueStatsUiModel?,
        statsTimeRangeSelection: StatsTimeRangeSelection
    ) {
        if (revenueStats?.intervalList.isNullOrEmpty()) {
            statsDateValue.visibility = View.GONE
        } else {
            val rangeType = statsTimeRangeSelection.selectionType
            val startInterval = revenueStats?.intervalList?.first()?.interval
            val startDate = startInterval?.let { getDateValueFromRangeType(it, rangeType) }

            val dateRangeString = when (rangeType) {
                WEEK_TO_DATE -> {
                    val endInterval = revenueStats?.intervalList?.last()?.interval
                    val endDate = endInterval?.let { getDateValueFromRangeType(it, rangeType) }
                    String.format(Locale.getDefault(), "%s – %s", startDate, endDate)
                }

                else -> startDate
            }
            if (statsTimeRangeSelection.selectionType != CUSTOM) statsDateValue.isVisible = true
            statsDateValue.text = dateRangeString
        }
    }

    /**
     * Method to get the date value for a given [dateString] based on the [rangeType]
     * This is used to populate the date bar when the **stats tab is loaded**
     * [TODAY] would be Tuesday, Aug 08
     * [WEEK_TO_DATE] would be Aug 4
     * [MONTH_TO_DATE] would be August
     * [YEAR_TO_DATE] would be 2019
     * [CUSTOM] Any of the above formats depending on the custom range length
     */
    private fun getDateValueFromRangeType(
        dateString: String,
        rangeType: SelectionType
    ): String {
        return when (rangeType) {
            TODAY -> dateUtils.getDayMonthDateString(dateString).orEmpty()
            WEEK_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            MONTH_TO_DATE -> dateUtils.getMonthString(dateString).orEmpty()
            YEAR_TO_DATE -> dateUtils.getYearString(dateString).orEmpty()
            CUSTOM -> getDateValueFromGranularity(dateString, statsTimeRangeSelection.revenueStatsGranularity)
            else -> error("Unsupported range value used in my store tab: $rangeType")
        }.also { result -> trackUnexpectedFormat(result, dateString) }
    }

    /**
     * Method to get the date value for a given [dateString] based on the [rangeType]
     * This is used to populate the date bar when the **stats tab is loaded**
     * [StatsGranularity.DAYS] would be Tuesday, Aug 08
     * [StatsGranularity.WEEKS] would be Aug 4
     * [StatsGranularity.MONTHS] would be August
     * [StatsGranularity.YEARS] would be 2019
     */
    private fun getDateValueFromGranularity(
        dateString: String,
        activeGranularity: StatsGranularity
    ): String {
        return when (activeGranularity) {
            StatsGranularity.HOURS -> dateUtils.getFriendlyDayHourString(dateString).orEmpty()
            StatsGranularity.DAYS -> dateUtils.getDayMonthDateString(dateString).orEmpty()
            StatsGranularity.WEEKS -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            StatsGranularity.MONTHS -> dateUtils.getMonthString(dateString).orEmpty()
            StatsGranularity.YEARS -> dateString
        }.also { result -> trackUnexpectedFormat(result, dateString) }
    }

    override fun onValueSelected(entry: Entry?, h: Highlight?) {
        if (entry == null) return
        // display the revenue for this entry
        val formattedRevenue = currencyFormatter.getFormattedAmountZeroRounded(
            entry.y.toDouble(),
            revenueStatsModel?.currencyCode.orEmpty()
        )
        revenueValue.text = formattedRevenue

        // display the order count for this entry
        val date = getDateFromIndex(entry.x.toInt())
        val orderCount = chartOrderStats[date]?.toInt() ?: 0
        ordersValue.text = orderCount.toString()
        updateVisitorsValue(date)
        updateConversionRate()
        updateDateOnScrubbing(date, statsTimeRangeSelection.selectionType)
        updateColorForStatsHeaderValues(R.color.color_secondary)
        onUserInteractionWithChart()
        statsDateValue.isVisible = true
    }

    private fun updateVisitorsValue(date: String) {
        if (statsTimeRangeSelection.selectionType == TODAY) {
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
     * Method to update the date value for a given [dateString] based on the [SelectionType]
     * This is used to display the date bar when the **scrubbing interaction is taking place**
     * [SelectionType.TODAY] would be Tuesday, Aug 08›7am
     * [SelectionType.WEEK_TO_DATE] would be Tuesday, Aug 08›7am
     * [SelectionType.MONTH_TO_DATE] would be Aug 08
     * [SelectionType.YEAR_TO_DATE] would be August›08
     * [SelectionType.CUSTOM] Any of the above formats depending on the custom range length
     */
    private fun updateDateOnScrubbing(dateString: String, rangeType: SelectionType) {
        statsDateValue.text = when (rangeType) {
            TODAY -> dateUtils.getFriendlyDayHourString(dateString).orEmpty()
            WEEK_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            MONTH_TO_DATE -> dateUtils.getLongMonthDayString(dateString).orEmpty()
            YEAR_TO_DATE -> dateUtils.getFriendlyLongMonthYear(dateString).orEmpty()
            CUSTOM -> getDisplayDateForGranularity(dateString, statsTimeRangeSelection.revenueStatsGranularity)

            else -> error("Unsupported range value used in my store tab: $rangeType")
        }.also { result -> trackUnexpectedFormat(result, dateString) }
    }

    /**
     * Returns a display date for the given date and granularity [StatsGranularity]
     * [StatsGranularity.HOURS] would be 7am, 8am, 9am
     * [StatsGranularity.DAYS] would be Aug 1, 2, 3
     * [StatsGranularity.WEEKS] would be 31 Jan, 5 Feb, 12 Feb, 19 Feb, 26 Feb
     * [StatsGranularity.MONTHS] would be Sept, Oct, Nov, Dec
     * [StatsGranularity.YEARS] would be 2019, 2020, 2021, 2022
     */
    private fun getDisplayDateForGranularity(dateString: String, statsGranularity: StatsGranularity): String =
        when (statsGranularity) {
            StatsGranularity.HOURS -> dateUtils.getShortHourString(dateString).orEmpty()
            StatsGranularity.DAYS -> dateUtils.getDayString(dateString).orEmpty()
            StatsGranularity.WEEKS -> dateUtils.getShortMonthDayStringForWeek(dateString).orEmpty()
            StatsGranularity.MONTHS -> dateUtils.getShortMonthString(dateString).orEmpty()
            StatsGranularity.YEARS -> dateString
        }.also { result -> trackUnexpectedFormat(result, dateString) }

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
        updateDate(revenueStatsModel, statsTimeRangeSelection)
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

    fun handleUnavailableVisitorStats() {
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = true
        binding.statsViewRow.visitorsValueTextview.isVisible = false
    }

    fun showLastUpdate(lastUpdateMillis: Long?) {
        if (lastUpdateMillis != null) {
            val lastUpdateFormatted = dateUtils.getDateOrTimeFromMillis(lastUpdateMillis)
            lastUpdated.isVisible = true
            fadeInLabelValue(
                lastUpdated,
                String.format(
                    Locale.getDefault(),
                    resources.getString(R.string.last_update),
                    lastUpdateFormatted
                )
            )
        } else {
            lastUpdated.isVisible = false
        }
    }

    @Suppress("MagicNumber")
    private fun updateConversionRate() {
        val ordersCount = ordersValue.text.toString().toIntOrNull()
        val visitorsCount = visitorsValue.text.toString().toIntOrNull()

        if (visitorsCount == null || ordersCount == null) {
            conversionValue.isVisible = false
            binding.statsViewRow.emptyConversionRateIndicator.isVisible = true
            return
        }
        val conversionRateDisplayValue = ordersCount convertedFrom visitorsCount
        val color = ContextCompat.getColor(context, R.color.color_on_surface_high)
        conversionValue.setTextColor(color)
        binding.statsViewRow.emptyConversionRateIndicator.isVisible = false
        conversionValue.isVisible = true
        conversionValue.text = conversionRateDisplayValue
    }

    fun clearStatsHeaderValues() {
        statsDateValue.text = ""
        lastUpdated.text = ""
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
        val revenue = currencyFormatter.getFormattedAmountZeroRounded(
            grossRevenue,
            revenueStatsModel?.currencyCode.orEmpty()
        )

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
        return if (statsTimeRangeSelection.selectionType == YEAR_TO_DATE) {
            visitorStats.mapKeys {
                dateUtils.getYearMonthString(it.key) ?: it.key.take("yyyy-MM".length)
            }
        } else {
            visitorStats
        }
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
    fun getStringForRangeType(rangeType: SelectionType): Int {
        return when (rangeType) {
            TODAY -> R.string.today
            WEEK_TO_DATE -> R.string.this_week
            MONTH_TO_DATE -> R.string.this_month
            YEAR_TO_DATE -> R.string.this_year
            CUSTOM -> R.string.orderfilters_date_range_filter_custom_range
            else -> error("Unsupported range value used in my store tab: $rangeType")
        }
    }

    private fun getStringForGranularity(granularity: StatsGranularity): String {
        val granularityLabel = when (granularity) {
            StatsGranularity.HOURS -> resources.getString(R.string.my_store_custom_range_granularity_hour)
            StatsGranularity.DAYS -> resources.getString(R.string.my_store_custom_range_granularity_day)
            StatsGranularity.WEEKS -> resources.getString(R.string.my_store_custom_range_granularity_week)
            StatsGranularity.MONTHS -> resources.getString(R.string.my_store_custom_range_granularity_month)
            StatsGranularity.YEARS -> resources.getString(R.string.my_store_custom_range_granularity_year)
        }
        return resources.getString(R.string.my_store_custom_range_granularity_label, granularityLabel)
    }

    private fun getEntryValueFromRangeType(dateString: String): String {
        return when (statsTimeRangeSelection.selectionType) {
            TODAY -> dateUtils.getShortHourString(dateString).orEmpty()
            WEEK_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            MONTH_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            YEAR_TO_DATE -> dateUtils.getShortMonthString(dateString).orEmpty()
            CUSTOM -> getEntryValuesForCustomType(dateString)
            else -> error("Unsupported range value used in my store tab: ${statsTimeRangeSelection.selectionType}")
        }.also { result -> trackUnexpectedFormat(result, dateString) }
    }

    private fun getEntryValuesForCustomType(dateString: String): String {
        return when (statsTimeRangeSelection.revenueStatsGranularity) {
            StatsGranularity.HOURS -> dateUtils.getShortHourString(dateString).orEmpty()
            StatsGranularity.DAYS -> dateUtils.getDayString(dateString).orEmpty()

            StatsGranularity.WEEKS -> dateUtils.getShortMonthDayString(dateString).orEmpty()
            StatsGranularity.MONTHS -> dateUtils.getShortMonthString(dateString).orEmpty()
            StatsGranularity.YEARS -> dateString
        }.also { result -> trackUnexpectedFormat(result, dateString) }
    }

    private fun trackUnexpectedFormat(result: String, dateString: String) {
        if (result.isEmpty()) {
            AnalyticsTracker.track(
                AnalyticsEvent.STATS_UNEXPECTED_FORMAT,
                mapOf(
                    KEY_DATE to dateString,
                    KEY_GRANULARITY to statsTimeRangeSelection.selectionType.identifier,
                    KEY_RANGE to revenueStatsModel?.rangeId
                )
            )
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
                    getEntryValueFromRangeType(dateString)
                } else {
                    getAxisLabelFromRangeType(dateString)
                }
            } else {
                ""
            }
        }

        /**
         * Displays the x-axis labels in the following format based on [SelectionType]
         * [SelectionType.TODAY] would be 7am, 8am, 9am
         * [SelectionType.WEEK_TO_DATE] would be 7am, 8am, 9am
         * [SelectionType.MONTH_TO_DATE] would be Aug 31, Sept 1, 2, 3
         * [SelectionType.YEAR_TO_DATE] would be Aug 1, 2, 3
         * [SelectionType.CUSTOM] Any of the above formats depending on the custom range length
         */
        private fun getAxisLabelFromRangeType(dateString: String): String {
            return when (statsTimeRangeSelection.selectionType) {
                TODAY -> dateUtils.getShortHourString(dateString).orEmpty()
                WEEK_TO_DATE -> getWeekLabelValue(dateString)
                MONTH_TO_DATE -> dateUtils.getDayString(dateString).orEmpty()
                YEAR_TO_DATE -> dateUtils.getShortMonthString(dateString).orEmpty()
                CUSTOM -> getDisplayDateForGranularity(dateString, statsTimeRangeSelection.revenueStatsGranularity)
                else -> error("Unsupported range value used in my store tab: ${statsTimeRangeSelection.selectionType}")
            }.also { result -> trackUnexpectedFormat(result, dateString) }
        }

        /**
         * Method returns the formatted date for the [StatsGranularity.WEEKS] tab,
         * if the date string is the first day of the month. i.e. date is equal to 1,
         * then the formatted date would be `MM-d` format.
         * Otherwise the formatted date would be `d` format
         */
        private fun getWeekLabelValue(dateString: String): String {
            val formattedDateString = dateUtils.getDayString(dateString)
            return if (formattedDateString == "1") {
                dateUtils.getShortMonthDayString(dateString).orEmpty()
            } else {
                formattedDateString.orEmpty()
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
