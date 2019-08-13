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

/**
 * Item decoration for recycler views which simply shows a vertical green bar to the left to
 * indicate unread items (such as unread notifications)
 */
class UnreadItemDecoration(context: Context, val decorListener: ItemDecorationListener) :
        DividerItemDecoration(context, HORIZONTAL) {
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
                val itemType = decorListener.getItemTypeAtPosition(position)
                /*
                 * note that we have to draw the indicator for all items rather than just unread ones
                 * in order to paint over recycled cells that have a previously-drawn indicator
                 */
                val colorId = when (itemType) {
                    ItemType.HEADER -> R.color.list_header_bg
                    ItemType.UNREAD -> R.color.wc_green
                    else -> R.color.list_item_bg
                }

                val paint = Paint()
                paint.color = ContextCompat.getColor(parent.context, colorId)

                parent.getDecoratedBoundsWithMargins(child, bounds)
                val top = bounds.top.toFloat()
                val bottom = (bounds.bottom + Math.round(child.translationY)).toFloat()
                val left = bounds.left.toFloat()
                val right = left + dividerWidth

                canvas.drawRect(left, top, right, bottom, paint)
            }
        }
    }
}
