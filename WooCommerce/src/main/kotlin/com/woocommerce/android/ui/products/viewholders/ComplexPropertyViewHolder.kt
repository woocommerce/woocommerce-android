package com.woocommerce.android.ui.products.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.WCProductPropertyView
import com.woocommerce.android.ui.products.models.ProductDetailItem.ComplexProperty

class ComplexPropertyViewHolder(parent: ViewGroup) : ProductDetailViewHolder(parent, R.layout.product_property_view) {
    fun bind(item: ComplexProperty) {
        val context = itemView.context

        val propertyView = View.inflate(context, R.layout.product_property_view, null) as WCProductPropertyView
        propertyView.show(
            LinearLayout.VERTICAL,
            context.getString(item.title ?: R.string.product_name),
            item.value,
            item.icon
        )

        propertyView.setMaxLines(1)
        propertyView.showPropertyName(item.title != null)

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
