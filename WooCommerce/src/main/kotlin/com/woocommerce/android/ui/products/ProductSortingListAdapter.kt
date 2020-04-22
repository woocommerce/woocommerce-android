package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductSortingListAdapter.ProductSortingViewHolder
import com.woocommerce.android.ui.products.ProductSortingListViewModel.SortingListItemUiModel
import kotlinx.android.synthetic.main.product_sorting_list_item.view.*
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting

class ProductSortingListAdapter(
    private val context: Context,
    private val onItemClicked: (option: ProductSorting) -> Unit
) : RecyclerView.Adapter<ProductSortingViewHolder>() {
    private val items = mutableListOf<SortingListItemUiModel>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductSortingViewHolder {
        return ProductSortingViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_sorting_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductSortingViewHolder, position: Int) {
        holder.bind(items[position], onItemClicked)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = items.size

    fun update(items: List<SortingListItemUiModel>) {
        val diffResult = DiffUtil.calculateDiff(ProductSortingItemDiffUtil(this.items.toList(), items))
        this.items.clear()
        this.items.addAll(items)

        diffResult.dispatchUpdatesTo(this)
    }

    class ProductSortingViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val txtSortingName: TextView = view.sortingItem_name
        private val txtSortingSelection: TextView = view.sortingItem_tick

        fun bind(item: SortingListItemUiModel, onItemClicked: (option: ProductSorting) -> Unit) {
            txtSortingName.text = view.context.getString(item.stringResource)
            txtSortingSelection.isSelected = item.isSelected
            view.setOnClickListener {
                onItemClicked(item.value)
            }
        }
    }

    private class ProductSortingItemDiffUtil(
        val items: List<SortingListItemUiModel>,
        val result: List<SortingListItemUiModel>
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
