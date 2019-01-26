package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.notifications.NotifsListItemDecoration.ItemType.READ_NOTIF
import org.wordpress.android.util.DisplayUtils

/**
 * Custom item decoration used to show an unread indicator next to unread notifs in the notifs list
 */
class NotifsListItemDecoration(context: Context) : DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL) {
    enum class ItemType {
        HEADER,
        UNREAD_NOTIF,
        READ_NOTIF
    }

    interface ItemDecorationListener {
        fun getItemTypeAtPosition(position: Int): ItemType
    }

    private val dividerWidth = DisplayUtils.dpToPx(context, 4).toFloat()
    private val headerPaint = Paint()
    private val unreadPaint = Paint()
    private val readPaint = Paint()
    private var listener: ItemDecorationListener? = null

    init {
        headerPaint.color = ContextCompat.getColor(context, R.color.list_header_bg)
        unreadPaint.color = ContextCompat.getColor(context, R.color.wc_green)
        readPaint.color = ContextCompat.getColor(context, R.color.list_item_bg)
    }

    fun setListener(listener: ItemDecorationListener?) {
        this.listener = listener
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            val itemType = listener?.getItemTypeAtPosition(position) ?: READ_NOTIF

            val left = child.left.toFloat()
            val right = left + dividerWidth
            val top = child.top.toFloat()
            val bottom = top + child.bottom.toFloat()

            val paint = when (itemType) {
                ItemType.HEADER -> headerPaint
                ItemType.UNREAD_NOTIF -> unreadPaint
                else -> readPaint
            }
            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}
