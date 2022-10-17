package com.woocommerce.android.ui.orders.filters.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.ui.orders.filters.model.OrderFilterCategoryUiModel

class OrderFilterCategoryAdapter(
    private val onFilterCategoryClicked: (OrderFilterCategoryUiModel) -> Unit
) : ListAdapter<OrderFilterCategoryUiModel, OrderFilterCategoryAdapter.OrderFilterCategoryViewHolder>(
    OrderFilterCategoryDiffCallBack
) {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderFilterCategoryViewHolder {
        return OrderFilterCategoryViewHolder(
            FilterListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holderCategory: OrderFilterCategoryViewHolder, position: Int) {
        holderCategory.bind(getItem(position))
        holderCategory.itemView.setOnClickListener {
            onFilterCategoryClicked(getItem(position))
        }
    }

    override fun getItemId(position: Int) = getItem(position).categoryKey.ordinal.toLong()

    class OrderFilterCategoryViewHolder(val viewBinding: FilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: OrderFilterCategoryUiModel) {
            viewBinding.filterItemName.text = filter.displayName
            viewBinding.filterItemSelection.text = filter.displayValue
        }
    }

    object OrderFilterCategoryDiffCallBack : DiffUtil.ItemCallback<OrderFilterCategoryUiModel>() {
        override fun areItemsTheSame(
            oldUiItemCategory: OrderFilterCategoryUiModel,
            newUiItemCategory: OrderFilterCategoryUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory

        override fun areContentsTheSame(
            oldUiItemCategory: OrderFilterCategoryUiModel,
            newUiItemCategory: OrderFilterCategoryUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory
    }
}
