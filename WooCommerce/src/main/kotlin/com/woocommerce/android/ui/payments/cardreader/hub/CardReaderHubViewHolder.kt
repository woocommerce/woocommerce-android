package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderHubListItemBinding
import com.woocommerce.android.databinding.CardReaderHubToggelableItemBinding
import com.woocommerce.android.util.UiHelpers

private const val DISABLED_BUTTON_ALPHA = 0.5f

sealed class CardReaderHubViewHolder(val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: CardReaderHubViewModel.CardReaderHubViewState.ListItem)

    class RowViewHolder(parent: ViewGroup) : CardReaderHubViewHolder(parent, R.layout.card_reader_hub_list_item) {
        var binding: CardReaderHubListItemBinding = CardReaderHubListItemBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewModel.CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewModel.CardReaderHubViewState.ListItem.NonTogglableListItem
            binding.cardReaderHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            binding.cardReaderMenuIcon.setImageResource(uiState.icon)

            if (uiState.isEnabled) {
                binding.root.setOnClickListener { uiState.onClick.invoke() }
                binding.cardReaderMenuIcon.alpha = 1.0f
                binding.cardReaderHubListItemLabelTv.alpha = 1.0f
            } else {
                binding.root.setOnClickListener(null)
                binding.cardReaderMenuIcon.alpha = DISABLED_BUTTON_ALPHA
                binding.cardReaderHubListItemLabelTv.alpha = DISABLED_BUTTON_ALPHA
            }
        }
    }

    class ToggelableViewHolder(parent: ViewGroup) :
        CardReaderHubViewHolder(parent, R.layout.card_reader_hub_toggelable_item) {
        var binding: CardReaderHubToggelableItemBinding = CardReaderHubToggelableItemBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewModel.CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewModel.CardReaderHubViewState.ListItem.TogglableListItem
            binding.cardReaderHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            binding.cardReaderMenuIcon.setImageResource(uiState.icon)

            binding.cardReaderHubListItemDescriptionTv.text = "Pay in Person lets you accept card or cash payments on collection or delivery"
        }

    }
}
