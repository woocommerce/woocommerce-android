package com.woocommerce.android.ui.products.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductDetailItem.Property

class PropertyViewHolder(parent: ViewGroup) : ProductDetailViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: Property) {
        val context = itemView.context
        val propertyView = View.inflate(context, R.layout.product_property_view, null) as WCProductPropertyView
        propertyView.show(LinearLayout.HORIZONTAL, context.getString(item.title), item.value)
    }
}
