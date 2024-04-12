package com.woocommerce.android.ui.products.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductFilterOptionListItemBinding
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListOptionItemUiModel
import com.woocommerce.android.ui.products.filter.ProductFilterOptionListAdapter.ProductFilterOptionViewHolder

class ProductFilterOptionListAdapter(
    private val clickListener: OnProductFilterOptionClickListener,
    private val loadMoreListener: OnLoadMoreListener
) : ListAdapter<FilterListOptionItemUiModel, ProductFilterOptionViewHolder>(FilterOptionDiffCallback) {
    init {
        setHasStableIds(true)
    }

    interface OnProductFilterOptionClickListener {
        fun onFilterOptionClick(selectedFilter: FilterListOptionItemUiModel)
    }

    fun updateData(uiModels: List<FilterListOptionItemUiModel>) {
        submitList(uiModels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterOptionViewHolder {
        return ProductFilterOptionViewHolder(
            ProductFilterOptionListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductFilterOptionViewHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener {
            clickListener.onFilterOptionClick(getItem(position))
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    class ProductFilterOptionViewHolder(val viewBinding: ProductFilterOptionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: FilterListOptionItemUiModel) {
            when (filter) {
                is FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel -> {
                    viewBinding.filterOptionItemName.apply {
                        if (filter.margin != 0) {
                            val newLayoutParams = layoutParams as ConstraintLayout.LayoutParams
                            newLayoutParams.marginStart = filter.margin
                            layoutParams = newLayoutParams
                        }
                        text = filter.filterOptionItemName
                    }
                    viewBinding.filterOptionItemTick.isVisible = filter.isSelected
                    viewBinding.explorePlugin.isVisible = false
                }
                is FilterListOptionItemUiModel.ExploreOptionItemUiModel -> {
                    viewBinding.filterOptionItemName.text = filter.filterOptionItemName
                    viewBinding.filterOptionItemTick.isVisible = false
                    viewBinding.explorePlugin.isVisible = true
                }
            }
        }
    }

    object FilterOptionDiffCallback : DiffUtil.ItemCallback<FilterListOptionItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: FilterListOptionItemUiModel,
            newItem: FilterListOptionItemUiModel
        ): Boolean {
            val areDefaultItemsTheSame = oldItem is FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel &&
                newItem is FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel &&
                oldItem.filterOptionItemValue == newItem.filterOptionItemValue

            val areExploreItemsTheSame = oldItem is FilterListOptionItemUiModel.ExploreOptionItemUiModel &&
                newItem is FilterListOptionItemUiModel.ExploreOptionItemUiModel &&
                oldItem.filterOptionItemName == newItem.filterOptionItemName

            return areDefaultItemsTheSame || areExploreItemsTheSame
        }

        override fun areContentsTheSame(
            oldItem: FilterListOptionItemUiModel,
            newItem: FilterListOptionItemUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
