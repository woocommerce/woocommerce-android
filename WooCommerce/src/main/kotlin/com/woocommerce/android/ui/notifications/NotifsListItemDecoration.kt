package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.View
import com.woocommerce.android.R
import org.wordpress.android.util.DisplayUtils

class NotifsListItemDecoration(context: Context) : DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL) {
    private val dividerWidth: Int = DisplayUtils.dpToPx(context, 10)
    private val divider: Drawable? = ContextCompat.getDrawable(context, R.drawable.notifs_unread_indicator)

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.right = outRect.left + dividerWidth
        outRect.bottom = view.layoutParams.height
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            val params = child.layoutParams as RecyclerView.LayoutParams
            val top = child.bottom + params.bottomMargin
            val bottom = top + params.height

            divider?.setBounds(0, 0, dividerWidth, bottom)
            divider?.draw(canvas)
        }
    }
}
