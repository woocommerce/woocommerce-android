package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.products_sort_and_filters_card.view.*

class ProductSortAndFiltersCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.products_sort_and_filters_card, this)
    }

    interface ProductSortAndFilterListener {
        fun onFilterOptionSelected()
        fun onSortOptionSelected()
    }

    fun initView(listener: ProductSortAndFilterListener) {
        btn_product_filter.setOnClickListener { listener.onFilterOptionSelected() }
        btn_product_sorting.setOnClickListener { listener.onSortOptionSelected() }
    }

    fun setSortingTitle(title: String) {
        btn_product_sorting.text = title
    }

    fun updateFilterSelection(filterCount: Int) {
        val isFilterEnabled = filterCount > 0
        with(btn_product_filter) {
            text = if (isFilterEnabled) {
                context.getString(R.string.product_list_filters_selected, filterCount)
            } else context.getString(R.string.product_list_filters)

            isSelected = isFilterEnabled
        }
    }
}
