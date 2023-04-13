package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.GapBetweenSections
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.LearnMoreListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.cardreader.hub.CardReaderHubViewState.ListItem.ToggleableListItem

class CardReaderHubAdapter :
    ListAdapter<CardReaderHubViewState.ListItem, CardReaderHubViewHolder>(ListItemDiffCallback) {

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
            is GapBetweenSections -> {
                VIEW_TYPE_GAP_BETWEEN_SECTIONS
            }
            is LearnMoreListItem -> {
                VIEW_TYPE_LEARN_MORE
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
            VIEW_TYPE_GAP_BETWEEN_SECTIONS -> {
                CardReaderHubViewHolder.GapBetweenSectionsViewHolder(parent)
            }
            VIEW_TYPE_LEARN_MORE -> {
                CardReaderHubViewHolder.LearnMoreViewHolder(parent)
            }
            else -> error("Unknown section")
        }
    }

    override fun onBindViewHolder(holder: CardReaderHubViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    fun setItems(rows: List<CardReaderHubViewState.ListItem>) {
        submitList(rows)
    }

    @Suppress("ReturnCount")
    object ListItemDiffCallback : DiffUtil.ItemCallback<CardReaderHubViewState.ListItem>() {
        override fun areItemsTheSame(
            oldItem: CardReaderHubViewState.ListItem,
            newItem: CardReaderHubViewState.ListItem
        ) = if (oldItem::class.java == newItem::class.java) {
            oldItem.label == newItem.label
        } else {
            false
        }

        override fun areContentsTheSame(
            oldItem: CardReaderHubViewState.ListItem,
            newItem: CardReaderHubViewState.ListItem
        ) = oldItem == newItem
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_TOGGELABLE = 1
        const val VIEW_TYPE_NON_TOGGELABLE = 2
        const val VIEW_TYPE_GAP_BETWEEN_SECTIONS = 3
        const val VIEW_TYPE_LEARN_MORE = 4
    }
}
