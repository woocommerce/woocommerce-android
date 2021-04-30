package com.woocommerce.android.widgets

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R

class DraggableItemTouchHelper(
    private val dragDirs: Int,
    private val onDragStarted: () -> Unit = {},
    private val onMove: (from: Int, to: Int) -> Unit
) : ItemTouchHelper(
        object : ItemTouchHelper.SimpleCallback(dragDirs, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder,
                target: ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                onMove(from, to)

                return true
            }

            override fun onSwiped(viewHolder: ViewHolder, direction: Int) {
                // no-op
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.setBackgroundColor(
                    ContextCompat.getColor(viewHolder.itemView.context, R.color.color_surface))
            }

            /*
             `onSelectedChanged` is triggered on any drag & drop related events
             for selected item on RecyclerView
            */
            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.setBackgroundColor(
                        ContextCompat.getColor(viewHolder.itemView.context, R.color.woo_purple_60_alpha_33))
                    onDragStarted()
                }
            }
        }
) {
    var isAttached: Boolean = false

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        super.attachToRecyclerView(recyclerView)
        isAttached = recyclerView != null
    }
}
