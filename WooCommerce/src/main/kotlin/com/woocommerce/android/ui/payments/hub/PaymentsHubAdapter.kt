package com.woocommerce.android.ui.payments.hub

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.GapBetweenSections
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.HeaderItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.LearnMoreListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.payments.hub.PaymentsHubViewState.ListItem.ToggleableListItem

class PaymentsHubAdapter :
    ListAdapter<PaymentsHubViewState.ListItem, PaymentsHubViewHolder>(ListItemDiffCallback) {

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
            is PaymentsHubViewState.ListItem.DepositSummaryListItem -> {
                VIEW_TYPE_DEPOSIT_SUMMARY
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentsHubViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOGGELABLE -> {
                PaymentsHubViewHolder.ToggleableViewHolder(parent)
            }
            VIEW_TYPE_NON_TOGGELABLE -> {
                PaymentsHubViewHolder.RowViewHolder(parent)
            }
            VIEW_TYPE_HEADER -> {
                PaymentsHubViewHolder.HeaderViewHolder(parent)
            }
            VIEW_TYPE_GAP_BETWEEN_SECTIONS -> {
                PaymentsHubViewHolder.GapBetweenSectionsViewHolder(parent)
            }
            VIEW_TYPE_LEARN_MORE -> {
                PaymentsHubViewHolder.LearnMoreViewHolder(parent)
            }
            VIEW_TYPE_DEPOSIT_SUMMARY -> {
                PaymentsHubViewHolder.DepositSummaryViewHolder(ComposeView(parent.context))
            }
            else -> error("Unknown section")
        }
    }

    override fun onBindViewHolder(holder: PaymentsHubViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    fun setItems(rows: List<PaymentsHubViewState.ListItem>) {
        submitList(rows)
    }

    @Suppress("ReturnCount")
    object ListItemDiffCallback : DiffUtil.ItemCallback<PaymentsHubViewState.ListItem>() {
        override fun areItemsTheSame(
            oldItem: PaymentsHubViewState.ListItem,
            newItem: PaymentsHubViewState.ListItem
        ) = if (oldItem::class.java == newItem::class.java) {
            oldItem.label == newItem.label
        } else {
            false
        }

        override fun areContentsTheSame(
            oldItem: PaymentsHubViewState.ListItem,
            newItem: PaymentsHubViewState.ListItem
        ) = oldItem == newItem
    }

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_TOGGELABLE = 1
        const val VIEW_TYPE_NON_TOGGELABLE = 2
        const val VIEW_TYPE_GAP_BETWEEN_SECTIONS = 3
        const val VIEW_TYPE_LEARN_MORE = 4
        const val VIEW_TYPE_DEPOSIT_SUMMARY = 5
    }
}
