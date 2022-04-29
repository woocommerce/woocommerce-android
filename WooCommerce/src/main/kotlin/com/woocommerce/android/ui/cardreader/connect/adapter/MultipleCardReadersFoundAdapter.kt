package com.woocommerce.android.ui.cardreader.connect.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.CardReaderListItem
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.ScanningInProgressListItem
import com.woocommerce.android.ui.cardreader.connect.adapter.MultipleCardReadersFoundViewHolder.CardReaderViewHolder
import com.woocommerce.android.ui.cardreader.connect.adapter.MultipleCardReadersFoundViewHolder.ScanningInProgressViewHolder

class MultipleCardReadersFoundAdapter : RecyclerView.Adapter<MultipleCardReadersFoundViewHolder>() {
    companion object {
        private const val VIEW_TYPE_READER_ITEM = 0
        private const val VIEW_TYPE_PROGRESS_ITEM = 1
        private const val ITEM_ID_PROGRESS_ITEM = -1L
    }

    init {
        setHasStableIds(true)
    }

    var list: List<ListItemViewState> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(MultipleCardReadersFoundDiffCallback(field, value), true)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is ScanningInProgressListItem -> VIEW_TYPE_PROGRESS_ITEM
            is CardReaderListItem -> VIEW_TYPE_READER_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MultipleCardReadersFoundViewHolder {
        return when (viewType) {
            VIEW_TYPE_PROGRESS_ITEM -> ScanningInProgressViewHolder(parent)
            VIEW_TYPE_READER_ITEM -> CardReaderViewHolder(parent)
            else -> {
                // Fail fast if a new view type is added so we can handle it
                throw IllegalStateException("The view type '$viewType' needs to be handled")
            }
        }
    }

    override fun onBindViewHolder(holder: MultipleCardReadersFoundViewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int = list.size

    override fun getItemId(position: Int): Long {
        return when (list[position]) {
            is ScanningInProgressListItem -> ITEM_ID_PROGRESS_ITEM
            is CardReaderListItem -> (list[position] as CardReaderListItem).readerId.hashCode().toLong()
        }
    }
}
