package com.woocommerce.android.ui.orders.filters

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.StringRes
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

    fun updateFilterSelection(filterCount: Int) {
        val isFilterEnabled = filterCount > 0
        with(binding.btnOrderFilter) {
            if (isFilterEnabled) {
                text = context.getString(R.string.product_list_filters_selected, filterCount)
                setFiltersTitle(R.string.orderfilters_filter_card_title_filtered_orders)
            } else {
                text = context.getString(R.string.product_list_filters)
                setFiltersTitle(R.string.orderfilters_filter_card_title_all_orders)
            }
            isSelected = isFilterEnabled
        }
    }

    private fun setFiltersTitle(@StringRes stringId: Int) {
        binding.filtersTitle.text = context.getString(stringId)
    }

    fun updateLastUpdate(value: String?) {
        binding.lastUpdate.apply {
            visibility = if (value.isNullOrEmpty()) View.GONE else View.VISIBLE
            text = value
        }
    }
}
