package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.mikephil.charting.charts.LineChart
import com.woocommerce.android.R
import kotlin.math.abs

/**
 * Creating a custom BarChart to fix this issue:
 * https://github.com/woocommerce/woocommerce-android/issues/1048
 */
class DashboardStatsLineChart(context: Context, attrs: AttributeSet) : LineChart(
    context,
    attrs
) {
    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.DashboardStatsBarChart, 0, 0)
        typedArray.recycle()
    }

    private val startTouchPoint = Point(0, 0)

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
}
