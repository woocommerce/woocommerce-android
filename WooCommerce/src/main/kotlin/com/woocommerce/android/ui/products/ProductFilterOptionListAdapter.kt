package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductFilterOptionListAdapter.ProductFilterChildViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListOptionItemUiModel
import kotlinx.android.synthetic.main.product_filter_option_list_item.view.*

class ProductFilterOptionListAdapter(
    private val clickListener: OnProductFilterChildClickListener
) : RecyclerView.Adapter<ProductFilterChildViewHolder>() {
    var filterList = listOf<FilterListOptionItemUiModel>()
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
        fun onChildFilterItemClick(selectedFilter: FilterListOptionItemUiModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterChildViewHolder {
        return ProductFilterChildViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_filter_option_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductFilterChildViewHolder, position: Int) {
        val filter = filterList[position]
        holder.txtFilterName.text = filter.filterOptionItemName

        val isChecked = filter.isSelected
        holder.selectedFilterItemRadioButton.visibility = if (isChecked) View.VISIBLE else View.GONE
        holder.selectedFilterItemRadioButton.isChecked = isChecked

        holder.itemView.setOnClickListener {
            clickListener.onChildFilterItemClick(filter)
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    private fun isSameList(newList: List<FilterListOptionItemUiModel>): Boolean {
        if (newList.size != filterList.size) {
            return false
        }

        for (index in newList.indices) {
            val oldItem = filterList[index]
            val newItem = newList[index]
            if (!oldItem.isSameFilterOption(newItem)) {
                return false
            }
        }
        return true
    }

    class ProductFilterChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterChildItem_name
        val selectedFilterItemRadioButton: RadioButton = view.filterChildItem_tick
    }
}
