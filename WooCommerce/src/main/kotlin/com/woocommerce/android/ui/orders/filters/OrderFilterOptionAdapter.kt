package com.woocommerce.android.ui.orders.filters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderFilterOptionItemBinding
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListOptionUiModel

class OrderFilterOptionAdapter(
    private val onFilterOptionClicked: (FilterListOptionUiModel) -> Unit
) : ListAdapter<FilterListOptionUiModel, OrderFilterOptionAdapter.OrderFilterOptionViewHolder>(
    OrderFilterOptionDiffCallBack
) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderFilterOptionViewHolder {
        return OrderFilterOptionViewHolder(
            OrderFilterOptionItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holderCategory: OrderFilterOptionViewHolder, position: Int) {
        holderCategory.bind(getItem(position))
        holderCategory.itemView.setOnClickListener {
            onFilterOptionClicked(getItem(position))
        }
    }

    class OrderFilterOptionViewHolder(val viewBinding: OrderFilterOptionItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(filterOption: FilterListOptionUiModel) {
            viewBinding.filterOptionItemName.text = filterOption.displayName
            viewBinding.filterOptionItemTick.isVisible = filterOption.isSelected
        }
    }

    object OrderFilterOptionDiffCallBack : DiffUtil.ItemCallback<FilterListOptionUiModel>() {

        override fun areItemsTheSame(
            oldUiItemCategory: FilterListOptionUiModel,
            newUiItemCategory: FilterListOptionUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory

        override fun areContentsTheSame(
            oldUiItemCategory: FilterListOptionUiModel,
            newUiItemCategory: FilterListOptionUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory
    }
}
