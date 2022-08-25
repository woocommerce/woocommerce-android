package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardReaderHubAdapter : RecyclerView.Adapter<CardReaderHubViewHolder>() {
    private val items = ArrayList<CardReaderHubViewModel.CardReaderHubViewState.ListItem>()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CardReaderHubViewModel.CardReaderHubViewState.ListItem.ToggleableListItem -> {
                VIEW_TYPE_TOGGELABLE
            }
            is CardReaderHubViewModel.CardReaderHubViewState.ListItem.NonToggleableListItem -> {
                VIEW_TYPE_NON_TOGGELABLE
            }
            is CardReaderHubViewModel.CardReaderHubViewState.ListItem.HeaderItem -> {
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
            else -> {
                throw IllegalStateException("Unknown section")
            }
        }
    }

    override fun onBindViewHolder(holder: CardReaderHubViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun setItems(rows: List<CardReaderHubViewModel.CardReaderHubViewState.ListItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    companion object {
        const val VIEW_TYPE_TOGGELABLE = 1
        const val VIEW_TYPE_NON_TOGGELABLE = 2
        const val VIEW_TYPE_HEADER = 0
    }
}
