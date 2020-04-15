package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductFilterListAdapter.ProductFilterViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUIModel
import kotlinx.android.synthetic.main.product_filter_list_item.view.*

class ProductFilterListAdapter(val context: Context) : RecyclerView.Adapter<ProductFilterViewHolder>() {
    private val filterList = mutableListOf<FilterListItemUIModel>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterViewHolder {
        return ProductFilterViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_filter_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductFilterViewHolder, position: Int) {
        val filter = filterList[position]
        holder.txtFilterName.text = context.getString(filter.key)

        // TODO: update the selected item in another commit
        holder.txtFilterSelection.text = context.getString(filter.childListItems[0].stringResource)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    fun setProductFilterList(newList: List<FilterListItemUIModel>) {
        val diffResult = DiffUtil.calculateDiff(ProductFilterItemDiffUtil(filterList.toList(), newList))
        filterList.clear()
        filterList.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    class ProductFilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterItemName
        val txtFilterSelection: TextView = view.filterItemSelection
    }

    private class ProductFilterItemDiffUtil(
        val items: List<FilterListItemUIModel>,
        val result: List<FilterListItemUIModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].key == result[newItemPosition].key

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.key == newItem.key
        }
    }
}
