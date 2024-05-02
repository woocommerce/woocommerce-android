package com.woocommerce.android.ui.common

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MarginBottomItemDecoration(
    marginTopResId: Int,
    context: Context
) : RecyclerView.ItemDecoration() {

    private val margin = context.resources.getDimensionPixelSize(marginTopResId)

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        outRect.bottom = margin
    }
}
