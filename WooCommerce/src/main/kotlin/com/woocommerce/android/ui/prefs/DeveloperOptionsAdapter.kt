package com.woocommerce.android.ui.prefs

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.NonToggleableListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.SpinnerListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem

class DeveloperOptionsAdapter : ListAdapter<ListItem, DeveloperOptionsViewHolder>(ListItemDiffCallback) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ToggleableListItem -> {
                VIEW_TYPE_TOGGLEABLE
            }
            is NonToggleableListItem -> {
                VIEW_TYPE_NON_TOGGLEABLE
            }
            is SpinnerListItem -> {
                VIEW_TYPE_RADIO_BUTTONS_DIALOG
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeveloperOptionsViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOGGLEABLE -> {
                DeveloperOptionsViewHolder.ToggleableViewHolder(parent)
            }

            VIEW_TYPE_NON_TOGGLEABLE -> {
                DeveloperOptionsViewHolder.RowViewHolder(parent)
            }

            VIEW_TYPE_RADIO_BUTTONS_DIALOG -> {
                DeveloperOptionsViewHolder.SpinnerViewHolder(parent)
            }
            else -> error("Unknown section")
        }
    }

    override fun onBindViewHolder(holder: DeveloperOptionsViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    fun setItems(rows: List<ListItem>) {
        submitList(rows)
    }

    @Suppress("ReturnCount")
    object ListItemDiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            if (oldItem is ToggleableListItem && newItem is ToggleableListItem) {
                return oldItem.label == newItem.label
            }
            if (oldItem is NonToggleableListItem && newItem is NonToggleableListItem) {
                return oldItem.label == newItem.label
            }
            return false
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        const val VIEW_TYPE_NON_TOGGLEABLE = 0
        const val VIEW_TYPE_TOGGLEABLE = 1
        const val VIEW_TYPE_RADIO_BUTTONS_DIALOG = 2
    }
}
