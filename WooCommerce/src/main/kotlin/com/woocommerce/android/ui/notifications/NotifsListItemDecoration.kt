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
    private var listener: ItemDecorationListener? = null

    fun setListener(listener: ItemDecorationListener?) {
        this.listener = listener
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)
            val itemType = listener?.getItemTypeAtPosition(position) ?: READ_NOTIF

            val colorId = when (itemType) {
                ItemType.HEADER -> R.color.list_header_bg
                ItemType.UNREAD_NOTIF -> R.color.wc_green
                else -> R.color.list_item_bg
            }

            val paint = Paint()
            paint.color = ContextCompat.getColor(parent.context, colorId)

            val left = child.left.toFloat()
            val right = left + dividerWidth
            val top = child.top.toFloat()
            val bottom = top + child.bottom.toFloat()

            canvas.drawRect(left, top, right, bottom, paint)
        }
    }
}
