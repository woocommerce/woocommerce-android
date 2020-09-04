package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.products.ProductFilterOptionListAdapter.ProductFilterOptionViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListOptionItemUiModel
import kotlinx.android.synthetic.main.product_filter_option_list_item.view.*

class ProductFilterOptionListAdapter(
    private val clickListener: OnProductFilterOptionClickListener
) : RecyclerView.Adapter<ProductFilterOptionViewHolder>() {
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

    interface OnProductFilterOptionClickListener {
        fun onFilterOptionClick(selectedFilter: FilterListOptionItemUiModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductFilterOptionViewHolder {
        return ProductFilterOptionViewHolder(LayoutInflater.from(parent.context)
                .inflate(layout.product_filter_option_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductFilterOptionViewHolder, position: Int) {
        val filter = filterList[position]
        holder.txtFilterName.text = filter.filterOptionItemName

        val isChecked = filter.isSelected
        holder.selectedFilterItemRadioButton.isVisible = isChecked
        holder.selectedFilterItemRadioButton.isChecked = isChecked

        holder.itemView.setOnClickListener {
            clickListener.onFilterOptionClick(filter)
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

    class ProductFilterOptionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtFilterName: TextView = view.filterOptionItem_name
        val selectedFilterItemRadioButton: RadioButton = view.filterOptionItem_tick
    }
}
