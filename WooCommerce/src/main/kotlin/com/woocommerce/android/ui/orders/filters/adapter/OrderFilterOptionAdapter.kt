package com.woocommerce.android.ui.orders.filters.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderFilterOptionItemBinding
import com.woocommerce.android.ui.orders.filters.model.OrderListFilterOptionUiModel

class OrderFilterOptionAdapter(
    private val onFilterOptionClicked: (OrderListFilterOptionUiModel) -> Unit
) : ListAdapter<OrderListFilterOptionUiModel, OrderFilterOptionAdapter.OrderFilterOptionViewHolder>(
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

    override fun getItemId(position: Int) = position.toLong()

    class OrderFilterOptionViewHolder(val viewBinding: OrderFilterOptionItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(orderFilterOption: OrderListFilterOptionUiModel) {
            viewBinding.filterOptionItemName.text = orderFilterOption.displayName
            viewBinding.filterOptionItemTick.isVisible = orderFilterOption.isSelected
        }
    }

    object OrderFilterOptionDiffCallBack : DiffUtil.ItemCallback<OrderListFilterOptionUiModel>() {
        override fun areItemsTheSame(
            oldUiItemCategoryFilter: OrderListFilterOptionUiModel,
            newUiItemCategoryFilter: OrderListFilterOptionUiModel
        ): Boolean = oldUiItemCategoryFilter == newUiItemCategoryFilter

        override fun areContentsTheSame(
            oldUiItemCategoryFilter: OrderListFilterOptionUiModel,
            newUiItemCategoryFilter: OrderListFilterOptionUiModel
        ): Boolean = oldUiItemCategoryFilter == newUiItemCategoryFilter
    }
}
