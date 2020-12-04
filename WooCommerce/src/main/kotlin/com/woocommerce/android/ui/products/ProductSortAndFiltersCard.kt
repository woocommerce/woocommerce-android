package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductsSortAndFiltersCardBinding

class ProductSortAndFiltersCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = ProductsSortAndFiltersCardBinding.inflate(LayoutInflater.from(ctx), this)

    interface ProductSortAndFilterListener {
        fun onFilterOptionSelected()
        fun onSortOptionSelected()
    }

    fun initView(listener: ProductSortAndFilterListener) {
        binding.btnProductFilter.setOnClickListener { listener.onFilterOptionSelected() }
        binding.btnProductSorting.setOnClickListener { listener.onSortOptionSelected() }
    }

    fun setSortingTitle(title: String) {
        binding.btnProductSorting.text = title
    }

    fun updateFilterSelection(filterCount: Int) {
        val isFilterEnabled = filterCount > 0
        with(binding.btnProductFilter) {
            text = if (isFilterEnabled) {
                context.getString(R.string.product_list_filters_selected, filterCount)
            } else context.getString(R.string.product_list_filters)

            isSelected = isFilterEnabled
        }
    }
}
