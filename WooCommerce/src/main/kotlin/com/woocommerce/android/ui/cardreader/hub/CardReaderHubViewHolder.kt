package com.woocommerce.android.ui.cardreader.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderHubListItemBinding
import com.woocommerce.android.util.UiHelpers

sealed class CardReaderHubViewHolder(val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: CardReaderHubViewModel.CardReaderHubListItemViewState)

    class RowViewHolder(parent: ViewGroup) : CardReaderHubViewHolder(parent, R.layout.card_reader_hub_list_item) {
        var binding: CardReaderHubListItemBinding = CardReaderHubListItemBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewModel.CardReaderHubListItemViewState) {
            binding.cardReaderHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            binding.cardReaderMenuIcon.setImageResource(uiState.icon)
            binding.root.setOnClickListener {
                uiState.onItemClicked.invoke()
            }
        }
    }
}
