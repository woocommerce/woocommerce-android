package com.woocommerce.android.ui.products.viewholders

import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductDetailItem.ComplexProperty

class ComplexPropertyViewHolder(parent: ViewGroup) : ProductDetailPropertyViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: ComplexProperty) {
        val context = itemView.context

        val propertyView = itemView as WCProductPropertyView
        propertyView.show(
            LinearLayout.VERTICAL,
            context.getString(item.title ?: R.string.product_name),
            item.value,
            item.icon
        )

        propertyView.setMaxLines(1)
        propertyView.showPropertyName(item.title != null && item.showTitle)

        if (item.onClick != null) {
            item.onClick.let { onClick ->
                propertyView.setClickListener {
                    onClick()
                }
            }
        } else {
            propertyView.removeClickListener()
        }
    }
}
