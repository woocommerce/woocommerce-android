package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductSortingListAdapter.ProductSortingViewHolder
import com.woocommerce.android.ui.products.ProductSortingViewModel.SortingListItemUIModel
import kotlinx.android.synthetic.main.product_sorting_list_item.view.*
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
        return ProductSortingViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_sorting_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductSortingViewHolder, position: Int) {
        holder.bind(options[position], onItemClicked, selectedOption)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = options.size

    class ProductSortingViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val txtSortingName: TextView = view.sortingItem_name
        private val txtSortingSelection: ImageView = view.sortingItem_tick

        fun bind(
            item: SortingListItemUIModel,
            onItemClicked: (option: ProductSorting) -> Unit,
            selectedOption: ProductSorting
        ) {
            txtSortingName.text = view.context.getString(item.stringResource)
            txtSortingSelection.isVisible = item.value == selectedOption
            view.setOnClickListener {
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
