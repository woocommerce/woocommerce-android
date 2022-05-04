package com.woocommerce.android.ui.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardReaderHubAdapter : RecyclerView.Adapter<CardReaderHubViewHolder>() {
    private val items = ArrayList<CardReaderHubViewModel.CardReaderHubListItemViewState>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardReaderHubViewHolder {
        return CardReaderHubViewHolder.RowViewHolder(parent)
    }

    override fun onBindViewHolder(holder: CardReaderHubViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun setItems(rows: List<CardReaderHubViewModel.CardReaderHubListItemViewState>) {
        items.clear()
        items.addAll(rows)
    }
}
