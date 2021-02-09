package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import org.wordpress.android.util.DisplayUtils
import kotlin.math.roundToInt

/**
 * Item decoration for recycler views which simply shows a vertical indicator bar to the left to
 * communicate unread items (such as unread product reviews)
 */
class UnreadItemDecoration(context: Context, private val decorListener: ItemDecorationListener) :
        DividerItemDecoration(context, VERTICAL) {
    interface ItemDecorationListener {
        fun getItemTypeAtPosition(position: Int): ItemType
    }

    enum class ItemType {
        HEADER,
        UNREAD,
        READ
    }

    private val dividerWidth = DisplayUtils.dpToPx(context, 3).toFloat()

    private val bounds = Rect()

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount - 1) {
            val child = parent.getChildAt(i)
            val position = child?.let { parent.getChildAdapterPosition(it) }
                    ?: SectionedRecyclerViewAdapter.INVALID_POSITION
            if (position != SectionedRecyclerViewAdapter.INVALID_POSITION) {
                /*
                 * note that we have to draw the indicator for all items rather than just unread ones
                 * in order to paint over recycled cells that have a previously-drawn indicator
                 */
                val colorId = when (decorListener.getItemTypeAtPosition(position)) {
                    ItemType.HEADER -> android.R.color.transparent
                    ItemType.UNREAD -> R.color.unread_indicator_color
                    else -> android.R.color.transparent
                }

                val paint = Paint()
                paint.color = ContextCompat.getColor(parent.context, colorId)

                parent.getDecoratedBoundsWithMargins(child, bounds)
                val top = bounds.top.toFloat()
                val bottom = (bounds.bottom + child.translationY.roundToInt()).toFloat()
                val left = bounds.left.toFloat()
                val right = left + dividerWidth

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }
}
