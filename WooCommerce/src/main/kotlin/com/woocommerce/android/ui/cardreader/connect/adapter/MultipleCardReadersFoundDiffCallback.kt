package com.woocommerce.android.ui.cardreader.connect.adapter

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.CardReaderListItem
import com.woocommerce.android.ui.cardreader.connect.CardReaderConnectViewModel.ListItemViewState.ScanningInProgressListItem

class MultipleCardReadersFoundDiffCallback(
    private val oldList: List<ListItemViewState>,
    private val newList: List<ListItemViewState>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is ScanningInProgressListItem && newItem == ScanningInProgressListItem -> true
            oldItem is CardReaderListItem && newItem is CardReaderListItem -> oldItem.readerId == newItem.readerId
            else -> false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is ScanningInProgressListItem && newItem == ScanningInProgressListItem -> true
            oldItem is CardReaderListItem && newItem is CardReaderListItem -> oldItem.readerId == newItem.readerId
            else -> false
        }
    }
}
