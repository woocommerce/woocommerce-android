package com.woocommerce.android.ui.orders.filters.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderFilterOptionItemBinding
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel

class OrderFilterOptionAdapter(
    private val onFilterOptionClicked: (OrderFilterOptionUiModel) -> Unit
) : ListAdapter<OrderFilterOptionUiModel, OrderFilterOptionAdapter.OrderFilterOptionViewHolder>(
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
        fun bind(orderFilterOption: OrderFilterOptionUiModel) {
            viewBinding.filterOptionItemName.text = orderFilterOption.displayName
            viewBinding.filterOptionItemTick.isVisible = orderFilterOption.isSelected
        }
    }

    object OrderFilterOptionDiffCallBack : DiffUtil.ItemCallback<OrderFilterOptionUiModel>() {
        override fun areItemsTheSame(
            oldUiItemCategoryFilter: OrderFilterOptionUiModel,
            newUiItemCategoryFilter: OrderFilterOptionUiModel
        ): Boolean = oldUiItemCategoryFilter == newUiItemCategoryFilter

        override fun areContentsTheSame(
            oldUiItemCategoryFilter: OrderFilterOptionUiModel,
            newUiItemCategoryFilter: OrderFilterOptionUiModel
        ): Boolean = oldUiItemCategoryFilter == newUiItemCategoryFilter
    }
}
