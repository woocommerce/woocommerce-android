package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductFilterListItemBinding
import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.ui.products.ProductFilterListAdapter.ProductFilterViewHolder
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListItemUiModel

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
        return ProductFilterViewHolder(
            ProductFilterListItemBinding.inflate(
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

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    private fun isSameList(newList: List<FilterListItemUiModel>) =
        filterList.areSameAs(newList) { this.isSameFilter(it) }

    class ProductFilterViewHolder(val viewBinding: ProductFilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: FilterListItemUiModel) {
            viewBinding.filterItemName.text = filter.filterItemName
            viewBinding.filterItemSelection.text =
                filter.filterOptionListItems.first { it.isSelected }.filterOptionItemName
        }
    }
}
