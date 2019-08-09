package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarEntry
import android.view.MotionEvent

/**
 * Creating a custom BarChart to fix this issue:
 * https://github.com/woocommerce/woocommerce-android/issues/1048
 */
class DashboardStatsBarChart(context: Context?, attrs: AttributeSet?) : BarChart(
        context,
        attrs
) {
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

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent.requestDisallowInterceptTouchEvent(data != null)
        super.onTouchEvent(event)
        return data != null
    }
}
