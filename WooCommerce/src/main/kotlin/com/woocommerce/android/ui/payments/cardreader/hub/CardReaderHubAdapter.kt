package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardReaderHubAdapter : RecyclerView.Adapter<CardReaderHubViewHolder>() {
    private val items = ArrayList<CardReaderHubViewModel.CardReaderHubViewState.ListItem>()

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int): Int {
        if (items[position] is CardReaderHubViewModel.CardReaderHubViewState.ListItem.TogglableListItem) {
            return VIEW_TYPE_TOGGELABLE
        }
        return VIEW_TYPE_NON_TOGGELABLE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardReaderHubViewHolder {
        if (viewType == VIEW_TYPE_NON_TOGGELABLE) {
            return CardReaderHubViewHolder.RowViewHolder(parent)
        }
        return CardReaderHubViewHolder.ToggelableViewHolder(parent)
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
    }
}
