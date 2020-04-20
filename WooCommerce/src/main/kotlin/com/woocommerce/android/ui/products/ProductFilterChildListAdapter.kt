package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductFilterChildListAdapter.ProductFilterChildViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListChildItemUiModel
import kotlinx.android.synthetic.main.product_filter_child_list_item.view.*

class ProductFilterChildListAdapter(
    private val clickListener: OnProductFilterChildClickListener
) : RecyclerView.Adapter<ProductFilterChildViewHolder>() {
    var filterList = listOf<FilterListChildItemUiModel>()
        set(value) {
            if (!isSameList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }

    init {
        setHasStableIds(true)
    }

    interface OnProductFilterChildClickListener {
        fun onChildFilterItemClick(selectedFilter: FilterListChildItemUiModel)
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

        holder.itemView.setOnClickListener {
            clickListener.onChildFilterItemClick(filter)
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    private fun isSameList(newList: List<FilterListChildItemUiModel>): Boolean {
        if (newList.size != filterList.size) {
            return false
        }

        newList.forEach {
            if (!isSameFilterChildItem(it)) {
                return false
            }
        }

        return true
    }

    private fun isSameFilterChildItem(filterChildItem: FilterListChildItemUiModel): Boolean {
        filterList.forEach {
            if (it.isSelected == filterChildItem.isSelected &&
                    it.filterChildItemName == filterChildItem.filterChildItemName &&
                    it.filterChildItemValue == filterChildItem.filterChildItemValue) {
                return true
            }
        }
        return false
    }

    class ProductFilterChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterChildItem_name
        val selectedFilterItemRadioButton: RadioButton = view.filterChildItem_tick
    }
}
