package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductFilterChildListAdapter.ProductFilterChildViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListChildItemUiModel
import kotlinx.android.synthetic.main.product_filter_child_list_item.view.*

class ProductFilterChildListAdapter() : RecyclerView.Adapter<ProductFilterChildViewHolder>() {
    private val filterList = mutableListOf<FilterListChildItemUiModel>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterChildViewHolder {
        return ProductFilterChildViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_filter_child_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductFilterChildViewHolder, position: Int) {
        val filter = filterList[position]
        holder.txtFilterName.text = filter.filterChildItemName

        val isChecked = filter.isSelected
        holder.selectedFilterItemRadioButton.visibility = if (isChecked) View.VISIBLE else View.GONE
        holder.selectedFilterItemRadioButton.isChecked = isChecked
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    fun setProductChildFilterList(newList: List<FilterListChildItemUiModel>) {
        val diffResult = DiffUtil.calculateDiff(ProductFilterChildItemDiffUtil(filterList.toList(), newList))
        filterList.clear()
        filterList.addAll(newList)

        diffResult.dispatchUpdatesTo(this)
    }

    class ProductFilterChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterChildItem_name
        val selectedFilterItemRadioButton: RadioButton = view.filterChildItem_tick
    }

    private class ProductFilterChildItemDiffUtil(
        val items: List<FilterListChildItemUiModel>,
        val result: List<FilterListChildItemUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                items[oldItemPosition].filterChildItemValue == result[newItemPosition].filterChildItemValue

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem.filterChildItemValue == newItem.filterChildItemValue
        }
    }
}
