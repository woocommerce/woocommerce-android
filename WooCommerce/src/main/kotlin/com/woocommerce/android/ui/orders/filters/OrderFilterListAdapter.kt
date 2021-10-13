package com.woocommerce.android.ui.orders.filters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListItemUiModel
import javax.inject.Inject

class OrderFilterListAdapter @Inject constructor(
    itemDiffCallBack: OrderFilterItemDiffCallBack
) : ListAdapter<FilterListItemUiModel, OrderFilterViewHolder>(itemDiffCallBack) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderFilterViewHolder {
        return OrderFilterViewHolder(
            FilterListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: OrderFilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class OrderFilterViewHolder(val viewBinding: FilterListItemBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(filter: FilterListItemUiModel) {
        viewBinding.filterItemName.text = filter.displayName
        viewBinding.filterItemSelection.text = filter.selectedValue
    }
}

class OrderFilterItemDiffCallBack @Inject constructor() : DiffUtil.ItemCallback<FilterListItemUiModel>() {

    override fun areItemsTheSame(
        oldUiItemItem: FilterListItemUiModel,
        newUiItemItem: FilterListItemUiModel
    ): Boolean = oldUiItemItem == newUiItemItem

    override fun areContentsTheSame(
        oldUiItemItem: FilterListItemUiModel,
        newUiItemItem: FilterListItemUiModel
    ): Boolean = oldUiItemItem == newUiItemItem
}
