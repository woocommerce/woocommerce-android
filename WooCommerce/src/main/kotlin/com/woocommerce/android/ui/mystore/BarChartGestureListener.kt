package com.woocommerce.android.ui.mystore

import android.view.MotionEvent
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener

/**
 * Interface that overrides the [OnChartGestureListener] class so as to NOT
 * implement unused methods to the implementing class
 */
interface BarChartGestureListener : OnChartGestureListener {
    override fun onChartLongPressed(me: MotionEvent?) {}
    override fun onChartSingleTapped(me: MotionEvent?) {}
    override fun onChartDoubleTapped(me: MotionEvent?) {}
    override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
    override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {}
    override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartGesture?) {}
    override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
}
