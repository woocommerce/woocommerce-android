package com.woocommerce.android.ui.prefs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DeveloperOptionsListItemBinding
import com.woocommerce.android.databinding.DeveloperOptionsTogglableItemBinding
import com.woocommerce.android.databinding.DeveloperOptionsUpdateReaderItemBinding
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.SpinnerListItem
import com.woocommerce.android.ui.prefs.DeveloperOptionsViewModel.DeveloperOptionsViewState.ListItem.ToggleableListItem
import com.woocommerce.android.util.UiHelpers

abstract class DeveloperOptionsViewHolder(val parent: ViewGroup, @LayoutRes layout: Int) :
    RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(layout, parent, false)) {
    abstract fun onBind(uiState: ListItem)

    class RowViewHolder(parent: ViewGroup) : DeveloperOptionsViewHolder(parent, R.layout.developer_options_list_item) {
        var binding: DeveloperOptionsListItemBinding = DeveloperOptionsListItemBinding.bind(itemView)
        override fun onBind(uiState: ListItem) {
            TODO("Not yet implemented")
        }
    }

    class ToggleableViewHolder(parent: ViewGroup) :
        DeveloperOptionsViewHolder(parent, R.layout.developer_options_togglable_item) {
        var binding: DeveloperOptionsTogglableItemBinding = DeveloperOptionsTogglableItemBinding.bind(itemView)
        override fun onBind(uiState: ListItem) {
            uiState as ToggleableListItem
            binding.developerOptionsToggleableListItemLabel.text = UiHelpers.getTextOfUiString(
                itemView.context,
                uiState.label
            )
            binding.developerOptionsToggleableIcon.setImageResource(uiState.icon)
            binding.developerOptionsSwitch.isEnabled = uiState.isEnabled
            binding.developerOptionsSwitch.isChecked = uiState.isChecked
            binding.developerOptionsSwitch.setOnCheckedChangeListener { _, isChecked ->
                if (uiState.isEnabled) {
                    uiState.onToggled(isChecked)
                }
            }
            binding.root.setOnClickListener {
                binding.developerOptionsSwitch.isChecked = !uiState.isChecked
            }
        }
    }

    class SpinnerViewHolder(parent: ViewGroup) :
        DeveloperOptionsViewHolder(parent, R.layout.developer_options_update_reader_item) {
        var binding: DeveloperOptionsUpdateReaderItemBinding = DeveloperOptionsUpdateReaderItemBinding.bind(itemView)
        override fun onBind(uiState: ListItem) {
            uiState as SpinnerListItem
            binding.developerOptionsSpinnerIcon.setImageResource(uiState.icon)
            binding.developerOptionsSpinnerEndIcon.setImageResource(uiState.endIcon)
            binding.developerOptionsSpinnerListItemLabel.text = UiHelpers.getTextOfUiString(
                itemView.context,
                uiState.label
            )
            binding.root.setOnClickListener {
                uiState.onClick.invoke()
            }
        }
    }
}
