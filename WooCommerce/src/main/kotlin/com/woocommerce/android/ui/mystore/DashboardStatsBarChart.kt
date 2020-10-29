package com.woocommerce.android.ui.mystore

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import com.woocommerce.android.R
import kotlin.math.abs

/**
 * Creating a custom BarChart to fix this issue:
 * https://github.com/woocommerce/woocommerce-android/issues/1048
 */
class DashboardStatsBarChart(context: Context, attrs: AttributeSet) : BarChart(
        context,
        attrs
) {
    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.DashboardStatsBarChart, 0, 0)
        try {
            setRadius(typedArray.getDimensionPixelSize(R.styleable.DashboardStatsBarChart_radius, 0).toFloat())
        } finally {
            typedArray.recycle()
        }
    }

    private val startTouchPoint = Point(0, 0)

    // Overriding this method from the Chart.java: line 719
    override fun drawMarkers(canvas: Canvas?) {
        // if there is no marker view or drawing marker is disabled
        if (mMarker == null || !isDrawMarkersEnabled || !valuesToHighlight())
            return

        for (i in mIndicesToHighlight.indices) {
            val highlight = mIndicesToHighlight[i]

            // This is the line that causes the crash
            val set = mData.getDataSetByIndex(highlight.dataSetIndex) ?: continue

            val e = mData.getEntryForHighlight(mIndicesToHighlight[i]) as? BarEntry ?: continue
            val entryIndex = set.getEntryIndex(e)

            // make sure entry not null
            if (entryIndex > set.entryCount * mAnimator.phaseX) {
                continue
            }

            val pos = getMarkerPosition(highlight)
            // check bounds
            if (!mViewPortHandler.isInBounds(pos[0], pos[1]))
                continue

            // callbacks to update the content
            mMarker.refreshContent(e, highlight)

            // draw the marker
            mMarker.draw(canvas, pos[0], pos[1])
        }
    }

    /**
     * Method added to prevent the chart's parent view (i.e ScrollView) from
     * intercepting the touch events during a horizontal scrubbing interaction
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (data != null) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchPoint.x = event.x.toInt()
                    startTouchPoint.y = event.y.toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val movement = Point(
                            abs(event.x.toInt() - startTouchPoint.x),
                            abs(event.y.toInt() - startTouchPoint.y)
                    )
                    // swallow the event if this is a horizontal scrub, which we determine by
                    // checking if the vertical motion is less than the horizontal motion
                    if (movement.y < movement.x) {
                        // see https://github.com/PhilJay/MPAndroidChart/issues/925
                        parent.requestDisallowInterceptTouchEvent(true)
                        super.onTouchEvent(event)
                        return true
                    }
                }
                // according to the docs, we must make sure to reset the intercept setting
                // when the user ends this event
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        return super.onTouchEvent(event)
    }

    private fun setRadius(radius: Float) {
        renderer = RoundedBarChartRenderer(
            this,
            animator,
            viewPortHandler,
            radius
        )
    }
}
