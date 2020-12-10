package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductSortingListItemBinding
import com.woocommerce.android.ui.products.ProductSortingListAdapter.ProductSortingViewHolder
import com.woocommerce.android.ui.products.ProductSortingViewModel.SortingListItemUIModel
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

class ProductSortingListAdapter(
    private val onItemClicked: (option: ProductSorting) -> Unit,
    private val options: List<SortingListItemUIModel>,
    private val selectedOption: ProductSorting
) : RecyclerView.Adapter<ProductSortingViewHolder>() {
    init {
        setHasStableIds(true)

        val diffResult = DiffUtil.calculateDiff(ProductSortingItemDiffUtil(this.options.toList(), options))
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductSortingViewHolder {
        return ProductSortingViewHolder(
            ProductSortingListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductSortingViewHolder, position: Int) {
        holder.bind(options[position], onItemClicked, selectedOption)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = options.size

    class ProductSortingViewHolder(val viewBinding: ProductSortingListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(
            item: SortingListItemUIModel,
            onItemClicked: (option: ProductSorting) -> Unit,
            selectedOption: ProductSorting
        ) {
            viewBinding.sortingItemName.text = itemView.context.getString(item.stringResource)
            viewBinding.sortingItemTick.isVisible = item.value == selectedOption
            itemView.setOnClickListener {
                onItemClicked(item.value)
            }
        }
    }

    private class ProductSortingItemDiffUtil(
        val items: List<SortingListItemUIModel>,
        val result: List<SortingListItemUIModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].value == result[newItemPosition].value

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem == newItem
        }
    }
}
