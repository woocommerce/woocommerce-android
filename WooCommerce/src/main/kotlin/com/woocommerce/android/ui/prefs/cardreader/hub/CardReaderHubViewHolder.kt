package com.woocommerce.android.ui.prefs.cardreader.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R

sealed class CardReaderHubViewHolder(val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: CardReaderHubViewModel.CardReaderHubListItemViewState)

    class RowViewHolder(parent: ViewGroup) : CardReaderHubViewHolder(parent, R.layout.card_reader_hub_list_item) {
        override fun onBind(uiState: CardReaderHubViewModel.CardReaderHubListItemViewState) {
        }
    }
}

