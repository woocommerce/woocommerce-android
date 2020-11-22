package com.woocommerce.android.ui.products.downloads

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

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

            /*
             `onSelectedChanged` is triggered on any drag & drop related events
             for selected item on RecyclerView
            */
            override fun onSelectedChanged(viewHolder: ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ACTION_STATE_DRAG) {
                    onDragStarted()
                }
            }
        }
)
