package com.woocommerce.android.ui.orders.filters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.ui.orders.filters.OrderFilterListViewModel.FilterListCategoryUiModel

class OrderFilterCategoryAdapter(
    private val onFilterCategoryClicked: (FilterListCategoryUiModel) -> Unit
) : ListAdapter<FilterListCategoryUiModel, OrderFilterCategoryAdapter.OrderFilterCategoryViewHolder>(
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

    override fun getItemId(position: Int) = position.toLong()

    class OrderFilterCategoryViewHolder(val viewBinding: FilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(filter: FilterListCategoryUiModel) {
            viewBinding.filterItemName.text = filter.displayName
            viewBinding.filterItemSelection.text = filter.displayValue
        }
    }

    object OrderFilterCategoryDiffCallBack : DiffUtil.ItemCallback<FilterListCategoryUiModel>() {

        override fun areItemsTheSame(
            oldUiItemCategory: FilterListCategoryUiModel,
            newUiItemCategory: FilterListCategoryUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory

        override fun areContentsTheSame(
            oldUiItemCategory: FilterListCategoryUiModel,
            newUiItemCategory: FilterListCategoryUiModel
        ): Boolean = oldUiItemCategory == newUiItemCategory
    }
}
