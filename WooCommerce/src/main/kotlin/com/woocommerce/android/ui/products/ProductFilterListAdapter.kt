package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.ui.products.ProductFilterListAdapter.ProductFilterViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUiModel
import kotlinx.android.synthetic.main.product_filter_list_item.view.*

class ProductFilterListAdapter(
    private val clickListener: OnProductFilterClickListener
) : RecyclerView.Adapter<ProductFilterViewHolder>() {
    var filterList = listOf<FilterListItemUiModel>()
        set(value) {
            if (!isSameList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }

    interface OnProductFilterClickListener {
        fun onProductFilterClick(selectedFilterPosition: Int)
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterViewHolder {
        return ProductFilterViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_filter_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductFilterViewHolder, position: Int) {
        val filter = filterList[position]
        holder.txtFilterName.text = filter.filterItemName
        holder.txtFilterSelection.text = filter.filterOptionListItems.first { it.isSelected }.filterOptionItemName

        holder.itemView.setOnClickListener {
            clickListener.onProductFilterClick(position)
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    private fun isSameList(newList: List<FilterListItemUiModel>): Boolean
        = filterList.areSameAs(newList) { this.isSameFilter(it) }

    class ProductFilterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterItemName
        val txtFilterSelection: TextView = view.filterItemSelection
    }
}
