package com.woocommerce.android.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.State
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

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: State) {
        parent.forEach { child ->
            val position = child.let { parent.getChildAdapterPosition(it) }
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

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        // Hide the default divider between review item
        outRect.setEmpty()
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
        // Draw nothing to hide divider line below review header
    }
}
