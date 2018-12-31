package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils

class NotifsListItemDecoration(context: Context) : DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL) {
    private val dividerWidth = DisplayUtils.dpToPx(context, 10).toFloat()
    private val dividerPaint = Paint()

    init {
        dividerPaint.color = ContextCompat.getColor(context, R.color.wc_green)
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state:  RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            val viewType = parent.adapter.getItemViewType(position)

            // a viewType of 0 is a header
            if (viewType > 0) {
                val left = child.left.toFloat()
                val right = left + dividerWidth
                val top = child.top.toFloat()
                val bottom = top + child.bottom.toFloat()

                canvas.drawRect(left, top, right, bottom, dividerPaint)
            }
        }
    }
}
