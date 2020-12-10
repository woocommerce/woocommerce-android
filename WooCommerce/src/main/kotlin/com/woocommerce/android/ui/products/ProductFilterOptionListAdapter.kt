package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductFilterOptionListItemBinding
import com.woocommerce.android.extensions.areSameAs
import com.woocommerce.android.ui.products.ProductFilterListViewModel.FilterListOptionItemUiModel
import com.woocommerce.android.ui.products.ProductFilterOptionListAdapter.ProductFilterOptionViewHolder

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
        return ProductFilterOptionViewHolder(
            ProductFilterOptionListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductFilterOptionViewHolder, position: Int) {
        holder.bind(filterList[position])
        holder.itemView.setOnClickListener {
            clickListener.onFilterOptionClick(filterList[position])
        }
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = filterList.size

    private fun isSameList(newList: List<FilterListOptionItemUiModel>) =
        filterList.areSameAs(newList) { this.isSameFilterOption(it) }

    class ProductFilterOptionViewHolder(val viewBinding: ProductFilterOptionListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(filter: FilterListOptionItemUiModel) {
            viewBinding.filterOptionItemName.text = filter.filterOptionItemName
            viewBinding.filterOptionItemTick.isVisible = filter.isSelected
            viewBinding.filterOptionItemTick.isChecked = filter.isSelected
        }
    }
}
