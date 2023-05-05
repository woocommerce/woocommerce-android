package com.woocommerce.android.ui.payments.cardreader.hub

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderHubHeaderBinding
import com.woocommerce.android.databinding.CardReaderHubListItemBinding
import com.woocommerce.android.databinding.CardReaderHubToggelableItemBinding
import com.woocommerce.android.databinding.CardReaderLearnMoreSectionBinding
import com.woocommerce.android.util.UiHelpers

private const val DISABLED_BUTTON_ALPHA = 0.5f

abstract class CardReaderHubViewHolder(val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: CardReaderHubViewState.ListItem)

    class RowViewHolder(parent: ViewGroup) : CardReaderHubViewHolder(parent, R.layout.card_reader_hub_list_item) {
        var binding: CardReaderHubListItemBinding = CardReaderHubListItemBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewState.ListItem.NonToggleableListItem
            binding.cardReaderHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            UiHelpers.setTextOrHide(binding.cardReaderHubListItemDescriptionTv, uiState.description)
            binding.cardReaderMenuIcon.setImageResource(uiState.icon)
            UiHelpers.setDrawableOrHide(
                binding.cardReaderHubBadgeIcon,
                uiState.iconBadge?.let { AppCompatResources.getDrawable(parent.context, it) }
            )

            if (uiState.isEnabled) {
                binding.root.setOnClickListener { uiState.onClick.invoke() }
                binding.cardReaderMenuIcon.alpha = 1.0f
                binding.cardReaderHubListItemLabelTv.alpha = 1.0f
            } else {
                binding.root.setOnClickListener(null)
                binding.cardReaderMenuIcon.alpha = DISABLED_BUTTON_ALPHA
                binding.cardReaderHubListItemLabelTv.alpha = DISABLED_BUTTON_ALPHA
            }

            (binding.cardReaderHubListItemDivider.layoutParams as ConstraintLayout.LayoutParams).startToStart =
                if (uiState.shortDivider) {
                    binding.cardReaderHubListItemLabelTv.id
                } else {
                    ConstraintLayout.LayoutParams.PARENT_ID
                }
        }
    }

    class ToggleableViewHolder(parent: ViewGroup) :
        CardReaderHubViewHolder(parent, R.layout.card_reader_hub_toggelable_item) {
        var binding: CardReaderHubToggelableItemBinding = CardReaderHubToggelableItemBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewState.ListItem.ToggleableListItem
            binding.cardReaderHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            binding.cardReaderMenuIcon.setImageResource(uiState.icon)
            UiHelpers.setTextOrHide(binding.cardReaderHubListItemDescriptionTv, uiState.description)
            binding.cardReaderHubSwitch.setOnCheckedChangeListener(null)
            binding.cardReaderHubSwitch.isEnabled = uiState.isEnabled
            binding.cardReaderHubSwitch.isClickable = uiState.isEnabled
            binding.cardReaderHubSwitch.isChecked = uiState.isChecked
            binding.cardReaderHubSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (uiState.isEnabled) {
                    uiState.onToggled(isChecked)
                }
            }
            binding.cardReaderHubListItemDescriptionTv.setOnClickListener {
                uiState.onLearnMoreClicked()
            }
            binding.root.setOnClickListener {
                binding.cardReaderHubSwitch.isChecked = !uiState.isChecked
            }
        }
    }

    class HeaderViewHolder(parent: ViewGroup) :
        CardReaderHubViewHolder(parent, R.layout.card_reader_hub_header) {
        var binding: CardReaderHubHeaderBinding = CardReaderHubHeaderBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewState.ListItem.HeaderItem
            binding.cardReaderHubHeaderTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
        }
    }

    class GapBetweenSectionsViewHolder(parent: ViewGroup) :
        CardReaderHubViewHolder(parent, R.layout.card_reader_hub_gap_between_sections) {
        override fun onBind(uiState: CardReaderHubViewState.ListItem) {
            // no-op
        }
    }

    class LearnMoreViewHolder(parent: ViewGroup) :
        CardReaderHubViewHolder(parent, R.layout.card_reader_learn_more_section) {

        var binding: CardReaderLearnMoreSectionBinding = CardReaderLearnMoreSectionBinding.bind(itemView)
        override fun onBind(uiState: CardReaderHubViewState.ListItem) {
            uiState as CardReaderHubViewState.ListItem.LearnMoreListItem
            UiHelpers.setTextOrHide(binding.learnMore, uiState.label)
            binding.learnMore.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(parent.context, uiState.icon),
                null,
                null,
                null
            )
            binding.learnMore.setOnClickListener { uiState.onClick?.invoke() }
            (binding.learnMore.layoutParams as MarginLayoutParams).topMargin = 0
        }
    }
}
