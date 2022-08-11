package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class CardReaderHubAdapter : RecyclerView.Adapter<CardReaderHubViewHolder>() {
    private val items = ArrayList<CardReaderHubViewModel.CardReaderHubViewState.ListItem>()

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardReaderHubViewHolder {
        return CardReaderHubViewHolder.RowViewHolder(parent)
    }

    override fun onBindViewHolder(holder: CardReaderHubViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    fun setItems(rows: List<CardReaderHubViewModel.CardReaderHubViewState.ListItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }
}
