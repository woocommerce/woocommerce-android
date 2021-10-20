package com.woocommerce.android.ui.orders.filters.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderFiltersCardBinding

class OrderFiltersCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderFiltersCardBinding.inflate(LayoutInflater.from(ctx), this)

    fun setClickListener(onFiltersSelected: () -> Unit) {
        binding.btnOrderFilter.setOnClickListener { onFiltersSelected() }
    }

    fun setFiltersTitle(title: String) {
        binding.filtersTitle.text = title
    }

    fun updateFilterSelection(filterCount: Int) {
        val isFilterEnabled = filterCount > 0
        with(binding.btnOrderFilter) {
            text = if (isFilterEnabled) {
                context.getString(R.string.product_list_filters_selected, filterCount)
            } else context.getString(R.string.product_list_filters)

            isSelected = isFilterEnabled
        }
    }
}
