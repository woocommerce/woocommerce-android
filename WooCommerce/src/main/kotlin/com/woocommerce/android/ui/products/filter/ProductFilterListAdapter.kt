package com.woocommerce.android.ui.products.filter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.ui.products.filter.ProductFilterListAdapter.ProductFilterViewHolder
import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListItemUiModel
import com.woocommerce.android.ui.products.filter.ProductFilterListViewModel.FilterListOptionItemUiModel

class ProductFilterListAdapter(
    private val clickListener: OnProductFilterClickListener
) : RecyclerView.Adapter<ProductFilterViewHolder>() {
    var filterList = listOf<FilterListItemUiModel>()
        set(value) {
            val diffResult =
                DiffUtil.calculateDiff(ProductFilterDiffUtil(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    interface OnProductFilterClickListener {
        fun onProductFilterClick(selectedFilterPosition: Int)
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterViewHolder {
        return ProductFilterViewHolder(
            FilterListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductFilterViewHolder, position: Int) {
        holder.bind(filterList[position])
        holder.itemView.setOnClickListener {
            clickListener.onProductFilterClick(position)
        }
    }

    override fun getItemId(position: Int) = filterList[position].filterItemKey.ordinal.toLong()

    override fun getItemCount() = filterList.size

    class ProductFilterViewHolder(val viewBinding: FilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: FilterListItemUiModel) {
            viewBinding.filterItemName.text = filter.filterItemName
            viewBinding.filterItemSelection.text =
                filter.filterOptionListItems
                    .filterIsInstance<FilterListOptionItemUiModel.DefaultFilterListOptionItemUiModel>()
                    .first { it.isSelected }.filterOptionItemName
        }
    }

    private class ProductFilterDiffUtil(
        val oldList: List<FilterListItemUiModel>,
        val newList: List<FilterListItemUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].filterItemKey == newList[newItemPosition].filterItemKey

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
