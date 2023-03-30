package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewModel.CardReaderHubViewState.ListItem.ToggleableListItem

class CardReaderHubAdapter :
    ListAdapter<CardReaderHubViewModel.CardReaderHubViewState.ListItem, CardReaderHubViewHolder>(ListItemDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ToggleableListItem -> {
                VIEW_TYPE_TOGGELABLE
            }
            is NonToggleableListItem -> {
                VIEW_TYPE_NON_TOGGELABLE
            }
            is HeaderItem -> {
                VIEW_TYPE_HEADER
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardReaderHubViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOGGELABLE -> {
                CardReaderHubViewHolder.ToggleableViewHolder(parent)
            }
            VIEW_TYPE_NON_TOGGELABLE -> {
                CardReaderHubViewHolder.RowViewHolder(parent)
            }
            VIEW_TYPE_HEADER -> {
                CardReaderHubViewHolder.HeaderViewHolder(parent)
            }
            else -> error("Unknown section")
        }
    }

    override fun onBindViewHolder(holder: CardReaderHubViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    fun setItems(rows: List<CardReaderHubViewModel.CardReaderHubViewState.ListItem>) {
        submitList(rows)
    }

    @Suppress("ReturnCount")
    object ListItemDiffCallback : DiffUtil.ItemCallback<CardReaderHubViewModel.CardReaderHubViewState.ListItem>() {
        override fun areItemsTheSame(
            oldItem: CardReaderHubViewModel.CardReaderHubViewState.ListItem,
            newItem: CardReaderHubViewModel.CardReaderHubViewState.ListItem
        ): Boolean {
            if (oldItem is HeaderItem && newItem is HeaderItem) {
                return oldItem.label == newItem.label
            }
            if (oldItem is ToggleableListItem && newItem is ToggleableListItem) {
                return oldItem.label == newItem.label
            }
            if (oldItem is NonToggleableListItem && newItem is NonToggleableListItem) {
                return (oldItem.label == newItem.label && oldItem.isEnabled == newItem.isEnabled)
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: CardReaderHubViewModel.CardReaderHubViewState.ListItem,
            newItem: CardReaderHubViewModel.CardReaderHubViewState.ListItem
        ): Boolean {
            return oldItem == newItem && (oldItem.isEnabled == newItem.isEnabled)
        }
    }

    companion object {
        const val VIEW_TYPE_TOGGELABLE = 1
        const val VIEW_TYPE_NON_TOGGELABLE = 2
        const val VIEW_TYPE_HEADER = 0
    }
}
