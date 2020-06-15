package com.woocommerce.android.ui.products.categories

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.add_product_category_view.view.*

class AddProductCategoryView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.add_product_category_view, this)
    }

    fun initView(callback: (View) -> Unit) {
        addCategoriesButton.setOnClickListener { view -> callback(view) }
    }
}
