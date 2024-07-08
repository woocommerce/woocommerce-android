package com.woocommerce.android.ui.dashboard.stats

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.doOnDetach
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_DATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_GRANULARITY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_RANGE
import com.woocommerce.android.databinding.MyStoreStatsBinding
import com.woocommerce.android.extensions.convertedFrom
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.ui.analytics.ranges.myStoreTrackingGranularityString
import com.woocommerce.android.ui.analytics.ranges.revenueStatsGranularity
import com.woocommerce.android.ui.dashboard.BarChartGestureListener
import com.woocommerce.android.ui.dashboard.DashboardStatsUsageTracksEventEmitter
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsViewModel.RevenueStatsUiModel
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsViewModel.VisitorStatsViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
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

@OptIn(FlowPreview::class)
@Suppress("MagicNumber")
class DashboardStatsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle
) : FrameLayout(ctx, attrs, defStyleAttr), OnChartValueSelectedListener, BarChartGestureListener {
    private val binding = MyStoreStatsBinding.inflate(LayoutInflater.from(ctx), this)

    companion object {
        private const val LINE_CHART_DOT_OFFSET = -5
        private const val EVENT_EMITTER_INTERACTION_DEBOUNCE = 1000L
    }

    private lateinit var statsTimeRangeSelection: StatsTimeRangeSelection
    private lateinit var dateUtils: DateUtils
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter

    private var revenueStatsModel: RevenueStatsUiModel? = null
    private var chartRevenueStats = mapOf<String, Double>()
    private var chartOrderStats = mapOf<String, Long>()
    private var visitorStatsState: VisitorStatsViewState = VisitorStatsViewState.NotLoaded

    private var skeletonView = SkeletonView()

    private var isRequestingStats = false
        set(value) {
            // if we're requesting chart data we clear the existing data so it doesn't continue
            // to appear, and we remove the chart's empty string so it doesn't briefly show
            // up before the chart data is added once the request completes
            if (value) {
                binding.chart.setNoDataText(null)
                binding.chart.clear()
            } else {
                // TODO add a custom empty view
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

    private lateinit var coroutineScope: CoroutineScope
    private val chartUserInteractions = MutableSharedFlow<Unit>()
    private lateinit var chartUserInteractionsJob: Job

    private var isChartValueSelected = false
    private lateinit var onDateSelected: (String?) -> Unit

    init {
        // TODO Remove those views from the layout when releasing Dynamic Dashboard
        customRangeLabel.isVisible = false
        statsDateValue.isVisible = false
        binding.statsTabLayout.isVisible = false
        customRangeButton.isVisible = false
        binding.viewAnalyticsButton.isVisible = false
    }

    @Suppress("LongParameterList")
    fun initView(
        dateUtils: DateUtils,
        currencyFormatter: CurrencyFormatter,
        usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter,
        lifecycleScope: LifecycleCoroutineScope,
        onViewAnalyticsClick: () -> Unit,
        onDateSelected: (String?) -> Unit
    ) {
        this.dateUtils = dateUtils
        this.currencyFormatter = currencyFormatter
        this.usageTracksEventEmitter = usageTracksEventEmitter
        this.coroutineScope = lifecycleScope
        this.onDateSelected = onDateSelected

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
                .collect {
                    usageTracksEventEmitter.interacted()

                    if (statsTimeRangeSelection.selectionType == SelectionType.CUSTOM) {
                        usageTracksEventEmitter.interactedWithCustomRange()
                    }
                }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        chartUserInteractionsJob.cancel()
    }

    fun loadDashboardStats(selectedTimeRange: StatsTimeRangeSelection) {
        onDateSelected(null)
        this.statsTimeRangeSelection = selectedTimeRange
        // Track range change
        AnalyticsTracker.track(
            AnalyticsEvent.DASHBOARD_MAIN_STATS_DATE,
            mapOf(KEY_RANGE to selectedTimeRange.myStoreTrackingGranularityString)
        )
        isRequestingStats = true
        applyCustomRange(statsTimeRangeSelection)
    }

    private fun applyCustomRange(selectedTimeRange: StatsTimeRangeSelection) {
        if (selectedTimeRange.selectionType == SelectionType.CUSTOM) {
            customRangeGranularityLabel.isVisible = true
            customRangeGranularityLabel.text = getStringForGranularity(selectedTimeRange.revenueStatsGranularity)
        } else {
            customRangeGranularityLabel.isVisible = false
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
            SelectionType.TODAY -> R.integer.stats_label_count_days
            SelectionType.WEEK_TO_DATE -> R.integer.stats_label_count_weeks
            SelectionType.MONTH_TO_DATE -> R.integer.stats_label_count_months
            SelectionType.YEAR_TO_DATE -> R.integer.stats_label_count_years
            SelectionType.CUSTOM -> R.integer.stats_label_count_custom_range
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
        onDateSelected(null)
        // update the total values of the chart here
        binding.chart.highlightValue(null)
        updateChartView()
        showTotalVisitorStats()
        updateColorForStatsHeaderValues(R.color.color_on_surface_high)
        onUserInteractionWithChart()
        isChartValueSelected = false
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
        onDateSelected(date)
        val orderCount = chartOrderStats[date]?.toInt() ?: 0
        ordersValue.text = orderCount.toString()
        updateVisitorsValue(date)
        updateConversionRate()
        updateColorForStatsHeaderValues(R.color.color_secondary)
        onUserInteractionWithChart()
        isChartValueSelected = true
    }

    private fun updateVisitorsValue(date: String) {
        val visitorStats = (visitorStatsState as? VisitorStatsViewState.Content)?.stats ?: return
        if (statsTimeRangeSelection.revenueStatsGranularity == StatsGranularity.HOURS) {
            // The visitor stats don't support hours granularity, so we need to hide them
            visitorsValue.isVisible = false
            visitorsValue.setText(R.string.emdash)
            binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = true
        } else {
            visitorsValue.isVisible = true
            binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = false
            visitorsValue.text = visitorStats[date]?.toString() ?: "0"
        }
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

    fun showVisitorStats(statsViewState: VisitorStatsViewState, rangeSelection: StatsTimeRangeSelection?) {
        rangeSelection?.let { statsTimeRangeSelection = it }
        // Reset click listeners
        binding.statsViewRow.emptyVisitorStatsIcon.setOnClickListener(null)
        binding.statsViewRow.emptyVisitorStatsIndicator.setOnClickListener(null)

        visitorStatsState = statsViewState.let {
            if (it is VisitorStatsViewState.Content) {
                it.copy(stats = getFormattedVisitorStats(it.stats))
            } else {
                it
            }
        }

        when (visitorStatsState) {
            is VisitorStatsViewState.Content -> showTotalVisitorStats()
            is VisitorStatsViewState.Error -> showVisitorStatsError()
            is VisitorStatsViewState.Unavailable -> showJetpackUnavailableVisitorStats()
            is VisitorStatsViewState.NotLoaded -> hideVisitorStats()
        }
    }

    private fun showVisitorStatsError() {
        binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = true
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = false
        binding.statsViewRow.visitorsValueTextview.isVisible = false
    }

    private fun showJetpackUnavailableVisitorStats() {
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = true
        binding.statsViewRow.visitorsValueTextview.isVisible = false
        binding.statsViewRow.emptyVisitorStatsIcon.apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_jetpack_logo))
            imageTintList = null
        }
    }

    private fun showTotalVisitorStats() {
        val visitorStatsState = visitorStatsState as? VisitorStatsViewState.Content ?: return
        when (visitorStatsState.totalVisitorCount) {
            null -> hideTotalVisitorCountForCustomRange()
            else -> {
                // Make sure the empty view is hidden
                binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = false
                fadeInLabelValue(visitorsValue, visitorStatsState.totalVisitorCount.toString())
            }
        }
    }

    private fun hideTotalVisitorCountForCustomRange() {
        fun showDialog() {
            val dialog = MaterialAlertDialogBuilder(context)
                .setTitle(R.string.my_store_custom_range_visitors_stats_unavailable_title)
                .setMessage(R.string.my_store_custom_range_visitors_stats_unavailable_message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show()

            doOnDetach {
                // To prevent leaking the dialog's window
                dialog.dismiss()
            }
        }

        binding.statsViewRow.visitorsValueTextview.isVisible = false
        binding.statsViewRow.emptyVisitorsStatsGroup.isVisible = true
        binding.statsViewRow.conversionValueTextView.isVisible = false
        binding.statsViewRow.emptyConversionRateIndicator.isVisible = true

        binding.statsViewRow.emptyVisitorStatsIcon.apply {
            setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_tintable_info_outline_24dp))
            imageTintList = ContextCompat.getColorStateList(context, R.color.color_primary)
            setOnClickListener { showDialog() }
        }

        binding.statsViewRow.emptyVisitorStatsIndicator.setOnClickListener {
            showDialog()
        }
    }

    private fun hideVisitorStats() {
        binding.statsViewRow.visitorsValueTextview.isVisible = false
        binding.statsViewRow.emptyVisitorStatsIndicator.isVisible = true
        binding.statsViewRow.emptyVisitorStatsIcon.isVisible = true
        binding.statsViewRow.conversionValueTextView.isVisible = false
        binding.statsViewRow.emptyConversionRateIndicator.isVisible = true
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
        val visitorsCount = visitorsValue.text.toString().toIntOrNull()?.takeIf {
            visitorsValue.isVisible
        }

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
        return if (statsTimeRangeSelection.selectionType == SelectionType.YEAR_TO_DATE) {
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
        val delay = duration.toMillis(context) + 200
        fadeHandler.postDelayed(
            {
                WooAnimUtils.fadeIn(view, duration)
                val color = ContextCompat.getColor(context, R.color.color_on_surface_high)
                view.setTextColor(color)
                view.text = value
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

    private fun trackUnexpectedFormat(result: String, dateString: String) {
        if (result.isEmpty()) {
            AnalyticsTracker.track(
                AnalyticsEvent.STATS_UNEXPECTED_FORMAT,
                mapOf(
                    KEY_DATE to dateString,
                    KEY_GRANULARITY to statsTimeRangeSelection.myStoreTrackingGranularityString,
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
                if (value == axis.mEntries.first() && statsTimeRangeSelection.selectionType != SelectionType.CUSTOM) {
                    getEntryValueForFirstItemOfXAxis(dateString)
                } else {
                    getAxisLabelFromRangeType(dateString)
                }
            } else {
                ""
            }
        }

        private fun getEntryValueForFirstItemOfXAxis(dateString: String): String {
            return when (statsTimeRangeSelection.selectionType) {
                SelectionType.TODAY -> dateUtils.getShortHourString(dateString).orEmpty()
                SelectionType.WEEK_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
                SelectionType.MONTH_TO_DATE -> dateUtils.getShortMonthDayString(dateString).orEmpty()
                SelectionType.YEAR_TO_DATE -> dateUtils.getShortMonthString(dateString).orEmpty()
                SelectionType.CUSTOM -> error("Custom range is unsupported to set a special x axis label")
                else -> error("Unsupported range value used in my store tab: ${statsTimeRangeSelection.selectionType}")
            }.also { result -> trackUnexpectedFormat(result, dateString) }
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
                SelectionType.TODAY -> dateUtils.getShortHourString(dateString).orEmpty()
                SelectionType.WEEK_TO_DATE -> getWeekLabelValue(dateString)
                SelectionType.MONTH_TO_DATE -> dateUtils.getDayString(dateString).orEmpty()
                SelectionType.YEAR_TO_DATE -> dateUtils.getShortMonthString(dateString).orEmpty()
                SelectionType.CUSTOM -> getDisplayDateForGranularity(
                    dateString,
                    statsTimeRangeSelection.revenueStatsGranularity
                )

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
