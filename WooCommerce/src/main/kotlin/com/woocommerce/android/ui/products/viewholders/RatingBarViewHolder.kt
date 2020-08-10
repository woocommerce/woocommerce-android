package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductProperty.RatingBar

class RatingBarViewHolder(parent: ViewGroup) : ProductPropertyViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: RatingBar) {
        val context = itemView.context
        val propertyView = itemView as WCProductPropertyView
        propertyView.show(LinearLayout.VERTICAL, context.getString(item.title), item.value, item.icon)
        propertyView.setRating(item.rating)
    }
}
