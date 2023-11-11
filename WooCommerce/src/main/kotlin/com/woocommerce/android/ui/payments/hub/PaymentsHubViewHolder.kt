package com.woocommerce.android.ui.payments.hub

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.CardReaderLearnMoreSectionBinding
import com.woocommerce.android.databinding.PaymentsHubHeaderBinding
import com.woocommerce.android.databinding.PaymentsHubListItemBinding
import com.woocommerce.android.databinding.PaymentsHubToggelableItemBinding
import com.woocommerce.android.ui.payments.hub.depositsummary.PaymentsHubDepositSummaryView
import com.woocommerce.android.util.UiHelpers

private const val DISABLED_BUTTON_ALPHA = 0.5f

abstract class PaymentsHubViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    abstract fun onBind(uiState: PaymentsHubViewState.ListItem)

    class RowViewHolder(parent: ViewGroup) : PaymentsHubViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.payments_hub_list_item, parent, false)
    ) {
        var binding = PaymentsHubListItemBinding.bind(itemView)
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            uiState as PaymentsHubViewState.ListItem.NonToggleableListItem
            binding.paymentsHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            UiHelpers.setTextOrHide(binding.paymentsHubListItemDescriptionTv, uiState.description)
            binding.paymentsHubMenuIcon.setImageResource(uiState.icon)
            UiHelpers.setDrawableOrHide(
                binding.paymentsHubBadgeIcon,
                uiState.iconBadge?.let { AppCompatResources.getDrawable(view.context, it) }
            )

            if (uiState.isEnabled) {
                binding.root.setOnClickListener { uiState.onClick.invoke() }
                binding.paymentsHubMenuIcon.alpha = 1.0f
                binding.paymentsHubListItemLabelTv.alpha = 1.0f
            } else {
                binding.root.setOnClickListener(null)
                binding.paymentsHubMenuIcon.alpha = DISABLED_BUTTON_ALPHA
                binding.paymentsHubListItemLabelTv.alpha = DISABLED_BUTTON_ALPHA
            }

            (binding.paymentsHubListItemDivider.layoutParams as ConstraintLayout.LayoutParams).startToStart =
                if (uiState.shortDivider) {
                    binding.paymentsHubListItemLabelTv.id
                } else {
                    ConstraintLayout.LayoutParams.PARENT_ID
                }
        }
    }

    class ToggleableViewHolder(parent: ViewGroup) :
        PaymentsHubViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.payments_hub_toggelable_item, parent, false)
        ) {
        var binding = PaymentsHubToggelableItemBinding.bind(itemView)
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            uiState as PaymentsHubViewState.ListItem.ToggleableListItem
            binding.paymentsHubListItemLabelTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
            binding.paymentsHubMenuIcon.setImageResource(uiState.icon)
            UiHelpers.setTextOrHide(binding.paymentsHubListItemDescriptionTv, uiState.description)
            binding.paymentsHubSwitch.setOnCheckedChangeListener(null)
            binding.paymentsHubSwitch.isEnabled = uiState.isEnabled
            binding.paymentsHubSwitch.isClickable = uiState.isEnabled
            binding.paymentsHubSwitch.isChecked = uiState.isChecked
            binding.paymentsHubSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (uiState.isEnabled) {
                    uiState.onToggled(isChecked)
                }
            }
            binding.paymentsHubListItemDescriptionTv.setOnClickListener {
                uiState.onLearnMoreClicked()
            }
            binding.root.setOnClickListener {
                binding.paymentsHubSwitch.isChecked = !uiState.isChecked
            }
        }
    }

    class HeaderViewHolder(parent: ViewGroup) :
        PaymentsHubViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.payments_hub_header, parent, false)
        ) {
        var binding = PaymentsHubHeaderBinding.bind(itemView)
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            uiState as PaymentsHubViewState.ListItem.HeaderItem
            binding.paymentsHubHeaderTv.text = UiHelpers.getTextOfUiString(itemView.context, uiState.label)
        }
    }

    class GapBetweenSectionsViewHolder(parent: ViewGroup) :
        PaymentsHubViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.payments_hub_gap_between_sections, parent, false)
        ) {
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            // no-op
        }
    }

    class LearnMoreViewHolder(parent: ViewGroup) :
        PaymentsHubViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.card_reader_learn_more_section, parent, false)
        ) {

        var binding: CardReaderLearnMoreSectionBinding = CardReaderLearnMoreSectionBinding.bind(itemView)
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            uiState as PaymentsHubViewState.ListItem.LearnMoreListItem
            UiHelpers.setTextOrHide(binding.learnMore, uiState.label)
            binding.learnMore.setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(view.context, uiState.icon),
                null,
                null,
                null
            )
            binding.learnMore.setOnClickListener { uiState.onClick?.invoke() }
            (binding.learnMore.layoutParams as MarginLayoutParams).topMargin = 0
        }
    }

    class DepositSummaryViewHolder(private val composeView: ComposeView) : PaymentsHubViewHolder(composeView) {
        override fun onBind(uiState: PaymentsHubViewState.ListItem) {
            composeView.setContent {
                PaymentsHubDepositSummaryView()
            }
        }
    }
}
