package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry

/**
 * Creating a custom BarChart to fix this issue:
 * https://github.com/woocommerce/woocommerce-android/issues/1048
 */
class DashboardStatsBarChart(context: Context?, attrs: AttributeSet?) : BarChart(
        context,
        attrs
) {
    private var offsetX = 0
    private var offsetY = 0
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
                    offsetX = 0
                    offsetY = 0
                    startTouchPoint.x = event.getX().toInt()
                    startTouchPoint.y = event.getY().toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val movement = Point(
                            Math.abs(event.getX().toInt() - startTouchPoint.x),
                            Math.abs(event.getY().toInt() - startTouchPoint.y)
                    )
                    // swallow the event if this is a horizontal scrub
                    if (movement.y < movement.x) {
                        // see https://github.com/PhilJay/MPAndroidChart/issues/925
                        parent.requestDisallowInterceptTouchEvent(true)
                        super.onTouchEvent(event)
                        return true
                    }
                }
                MotionEvent.ACTION_UP -> {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
            }
        }

        return super.onTouchEvent(event)
    }
}
