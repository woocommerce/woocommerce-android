package com.woocommerce.android.ui.mystore

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.ceil

/**
 * Custom [BarChartRenderer] to handle rounded corner bars for our stats screen. This custom class takes in
 * an additional param [mRadius] which is used to determine the radius of the rounded corners.
 *
 * Round corner bars are not supported in MPAndroidChart lib and based on some suggestions from
 * StackOverFlow, the logic is to override the [drawDataSet] method of the [BarChartRenderer] class
 * and draw the bars with rounded corners:
 *
 * https://stackoverflow.com/questions/30761082/mpandroidchart-round-edged-bar-chart
 */
class RoundedBarChartRenderer internal constructor(
    chart: BarDataProvider?,
    animator: ChartAnimator?,
    viewPortHandler: ViewPortHandler?,
    private val mRadius: Float
) : BarChartRenderer(chart, animator, viewPortHandler) {
    private val mBarShadowRectBuffer: RectF = RectF()

    override fun drawHighlighted(c: Canvas, indices: Array<Highlight>) {
        val barData = mChart.barData
        for (high in indices) {
            val set = barData.getDataSetByIndex(high.dataSetIndex)
            if (set == null || !set.isHighlightEnabled) continue

            val e = set.getEntryForXValue(high.x, high.y)
            if (!isInBoundsX(e, set)) continue

            val trans = mChart.getTransformer(set.axisDependency)
            mHighlightPaint.color = set.highLightColor
            mHighlightPaint.alpha = set.highLightAlpha

            val isStack = high.stackIndex >= 0 && e.isStacked
            val y1: Float
            val y2: Float
            if (isStack) {
                if (mChart.isHighlightFullBarEnabled) {
                    y1 = e.positiveSum
                    y2 = -e.negativeSum
                } else {
                    val range = e.ranges[high.stackIndex]
                    y1 = range.from
                    y2 = range.to
                }
            } else {
                y1 = e.y
                y2 = 0f
            }
            prepareBarHighlight(e.x, y1, y2, barData.barWidth / 2f, trans)
            setHighlightDrawPos(high, mBarRect)
            c.drawRoundRect(mBarRect, mRadius, mRadius, mHighlightPaint)
        }
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)
        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = Utils.convertDpToPixel(dataSet.barBorderWidth)
        val drawBorder = dataSet.barBorderWidth > 0f
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        // draw the bar shadow before the values
        if (mChart.isDrawBarShadowEnabled) {
            mShadowPaint.color = dataSet.barShadowColor
            val barData = mChart.barData
            val barWidth = barData.barWidth
            val barWidthHalf = barWidth / 2.0f
            var x: Float
            var i = 0
            val count = ceil(dataSet.entryCount.toFloat() * phaseX.toDouble()).toInt()
                    .coerceAtMost(dataSet.entryCount)

            while (i < count) {
                val e = dataSet.getEntryForIndex(i)
                x = e.x
                mBarShadowRectBuffer.left = x - barWidthHalf
                mBarShadowRectBuffer.right = x + barWidthHalf
                trans.rectValueToPixel(mBarShadowRectBuffer)
                if (!mViewPortHandler.isInBoundsLeft(mBarShadowRectBuffer.right)) {
                    i++
                    continue
                }
                if (!mViewPortHandler.isInBoundsRight(mBarShadowRectBuffer.left)) {
                    break
                }

                mBarShadowRectBuffer.top = mViewPortHandler.contentTop()
                mBarShadowRectBuffer.bottom = mViewPortHandler.contentBottom()
                c.drawRoundRect(mBarShadowRectBuffer, mRadius, mRadius, mShadowPaint)
                i++
            }
        }

        // initialize the buffer
        val buffer = mBarBuffers[index]
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)
        trans.pointValuesToPixel(buffer.buffer)
        val isSingleColor = dataSet.colors.size == 1
        if (isSingleColor) {
            mRenderPaint.color = dataSet.color
        }
        var j = 0
        while (j < buffer.size()) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break
            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(j / 4)
            }

            // Since the bar chart can contain negative and positive values, we first get the current y axis value
            // that is being rendered here.If the value is negative, only the bottom right and bottom left corners
            // of the bar is rounded. Similarly, if the value is positive, the top right and top left corners of
            // the bar is rounded.
            val barEntry = dataSet.getEntryForIndex(j / 4)
            val corners = if (barEntry != null && barEntry.y < 0) {
                floatArrayOf(
                        0f, 0f, // Top left corner
                        0f, 0f, // Top right corner
                        mRadius, mRadius, // Bottom right corner
                        mRadius, mRadius // Bottom left corner
                )
            } else {
                floatArrayOf(
                        mRadius, mRadius, // Top left corner
                        mRadius, mRadius, // Top right corner
                        0f, 0f, // Bottom right corner
                        0f, 0f // Bottom left corner
                )
            }

            val path = Path()
            path.addRoundRect(
                    RectF(buffer.buffer[j], buffer.buffer[j + 1], buffer.buffer[j + 2],
                            buffer.buffer[j + 3]), corners, Path.Direction.CW)
            c.drawPath(path, mRenderPaint)

            if (drawBorder) {
                c.drawPath(path, mBarBorderPaint)
            }
            j += 4
        }
    }
}
