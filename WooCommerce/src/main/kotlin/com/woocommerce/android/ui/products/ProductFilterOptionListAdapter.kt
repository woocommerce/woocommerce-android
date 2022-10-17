package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductFilterOptionListItemBinding
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListOptionItemUiModel
import com.woocommerce.android.ui.products.ProductFilterOptionListAdapter.ProductFilterOptionViewHolder

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

    override fun getItemId(position: Int) = getItem(position).filterOptionItemValue.hashCode().toLong()

    class ProductFilterOptionViewHolder(val viewBinding: ProductFilterOptionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: FilterListOptionItemUiModel) {
            viewBinding.filterOptionItemName.apply {
                if (filter.margin != 0) {
                    val newLayoutParams = layoutParams as LayoutParams
                    newLayoutParams.marginStart = filter.margin
                    layoutParams = newLayoutParams
                }
                text = filter.filterOptionItemName
            }
            viewBinding.filterOptionItemTick.isVisible = filter.isSelected
        }
    }

    object FilterOptionDiffCallback : DiffUtil.ItemCallback<FilterListOptionItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: FilterListOptionItemUiModel,
            newItem: FilterListOptionItemUiModel
        ): Boolean {
            return oldItem.filterOptionItemValue == newItem.filterOptionItemValue
        }

        override fun areContentsTheSame(
            oldItem: FilterListOptionItemUiModel,
            newItem: FilterListOptionItemUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
